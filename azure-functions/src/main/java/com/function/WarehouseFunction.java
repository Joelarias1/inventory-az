package com.function;

import java.util.*;
import java.sql.*;
import java.util.Date;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.ExecutionResult;
import graphql.schema.DataFetcher;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

/**
 * Azure Functions para CRUD de Bodegas con soporte REST y GraphQL
 *
 * Endpoints REST:
 * GET /api/WarehouseFunction - Listar todas las bodegas
 * GET /api/WarehouseFunction?id={id} - Obtener bodega por ID
 * POST /api/WarehouseFunction - Crear nueva bodega
 * PUT /api/WarehouseFunction?id={id} - Actualizar bodega
 * DELETE /api/WarehouseFunction?id={id} - Eliminar bodega
 *
 * Endpoint GraphQL:
 * POST /api/WarehouseFunction/graphql - Endpoint GraphQL
 */
public class WarehouseFunction {

    // Configuración de base de datos
    private static final String DB_URL = System.getenv("POSTGRES_URL") != null ?
        System.getenv("POSTGRES_URL") : "jdbc:postgresql://104.208.158.85:5432/duoc?sslmode=require";
    private static final String DB_USER = System.getenv("POSTGRES_USER") != null ?
        System.getenv("POSTGRES_USER") : "duoc";
    private static final String DB_PASSWORD = System.getenv("POSTGRES_PASSWORD") != null ?
        System.getenv("POSTGRES_PASSWORD") : "duoc1234";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GraphQL graphQL;

    public WarehouseFunction() {
        this.graphQL = createGraphQL();
    }

    @FunctionName("WarehouseFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req",
                        methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "WarehouseFunction/{action=rest}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("action") String action,
            final ExecutionContext context) {

        context.getLogger().info("WarehouseFunction procesando request: " + request.getHttpMethod() + " - Action: " + action);

        // Si la acción es "graphql", procesar como GraphQL
        if ("graphql".equalsIgnoreCase(action)) {
            return handleGraphQL(request, context);
        }

        // Procesar como REST API
        try {
            switch (request.getHttpMethod()) {
                case GET:
                    return handleGet(request, context);
                case POST:
                    return handlePost(request, context);
                case PUT:
                    return handlePut(request, context);
                case DELETE:
                    return handleDelete(request, context);
                default:
                    return createErrorResponse(request, "Método no soportado", 405);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en WarehouseFunction: " + e.getMessage());
            return createErrorResponse(request, "Error interno: " + e.getMessage(), 500);
        }
    }

    // ==================== MÉTODOS REST ====================

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        String testParam = request.getQueryParameters().get("test");

        // Endpoint de prueba sin BD
        if (testParam != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "WarehouseFunction funcionando correctamente");
            response.put("timestamp", new Date());
            response.put("test", true);
            response.put("supports", Arrays.asList("REST", "GraphQL"));
            return createSuccessResponse(request, response);
        }

        try (Connection conn = getConnection()) {
            if (idParam != null) {
                return getWarehouseById(conn, idParam, request, context);
            } else {
                return getAllWarehouses(conn, request, context);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en GET: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener bodegas: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage getAllWarehouses(Connection conn, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        String sql = "SELECT id, nombre, direccion, telefono, email, responsable, estado, capacidad_max, creado_en, modificado_en FROM bodegas ORDER BY id";

        List<Map<String, Object>> warehouses = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                warehouses.add(mapResultSetToWarehouse(rs));
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", warehouses);
        response.put("total", warehouses.size());
        response.put("message", "Bodegas obtenidas exitosamente");
        response.put("timestamp", new Date());

        return createSuccessResponse(request, response);
    }

    private HttpResponseMessage getWarehouseById(Connection conn, String idParam, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        try {
            int id = Integer.parseInt(idParam);
            String sql = "SELECT id, nombre, direccion, telefono, email, responsable, estado, capacidad_max, creado_en, modificado_en FROM bodegas WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> warehouse = mapResultSetToWarehouse(rs);

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", warehouse);
                        response.put("message", "Bodega encontrada exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Bodega no encontrada", 404);
                    }
                }
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(request, "ID inválido", 400);
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String body = request.getBody().orElse("{}");
        context.getLogger().info("Creando bodega con datos: " + body);

        try {
            Map<String, Object> warehouseData = objectMapper.readValue(body, Map.class);

            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO bodegas (nombre, direccion, telefono, email, responsable, estado, capacidad_max) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) warehouseData.getOrDefault("nombre", "Nueva Bodega"));
                    stmt.setString(2, (String) warehouseData.getOrDefault("direccion", "Dirección por defecto"));
                    stmt.setString(3, (String) warehouseData.getOrDefault("telefono", "123456789"));
                    stmt.setString(4, (String) warehouseData.getOrDefault("email", "bodega@empresa.com"));
                    stmt.setString(5, (String) warehouseData.getOrDefault("responsable", "Sistema"));
                    stmt.setString(6, (String) warehouseData.getOrDefault("estado", "ACTIVO"));
                    stmt.setInt(7, ((Number) warehouseData.getOrDefault("capacidad_max", 1000)).intValue());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int newId = rs.getInt("id");

                            Map<String, Object> response = new HashMap<>();
                            response.put("success", true);
                            response.put("data", Map.of("id", newId, "message", "Bodega creada exitosamente"));
                            response.put("message", "Bodega creada exitosamente");
                            response.put("timestamp", new Date());

                            return createSuccessResponse(request, response);
                        }
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en POST: " + e.getMessage());
            return createErrorResponse(request, "Error al crear bodega: " + e.getMessage(), 500);
        }

        return createErrorResponse(request, "Error al crear bodega", 500);
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para actualizar", 400);
        }

        String body = request.getBody().orElse("{}");

        try {
            Map<String, Object> warehouseData = objectMapper.readValue(body, Map.class);
            int id = Integer.parseInt(idParam);

            try (Connection conn = getConnection()) {
                String sql = "UPDATE bodegas SET nombre = ?, direccion = ?, estado = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) warehouseData.getOrDefault("nombre", "Bodega Actualizada"));
                    stmt.setString(2, (String) warehouseData.getOrDefault("direccion", "Dirección actualizada"));
                    stmt.setString(3, (String) warehouseData.getOrDefault("estado", "ACTIVO"));
                    stmt.setInt(4, id);

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", Map.of("id", id, "message", "Bodega actualizada exitosamente"));
                        response.put("message", "Bodega actualizada exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Bodega no encontrada", 404);
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en PUT: " + e.getMessage());
            return createErrorResponse(request, "Error al actualizar bodega: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para eliminar", 400);
        }

        try (Connection conn = getConnection()) {
            int id = Integer.parseInt(idParam);
            String sql = "DELETE FROM bodegas WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of("id", id, "message", "Bodega eliminada exitosamente"));
                    response.put("message", "Bodega eliminada exitosamente");
                    response.put("timestamp", new Date());

                    return createSuccessResponse(request, response);
                } else {
                    return createErrorResponse(request, "Bodega no encontrada", 404);
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en DELETE: " + e.getMessage());
            return createErrorResponse(request, "Error al eliminar bodega: " + e.getMessage(), 500);
        }
    }

    // ==================== MÉTODOS GRAPHQL ====================

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handleGraphQL(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        // Para GET requests en /graphql, mostrar información del schema
        if (request.getHttpMethod() == HttpMethod.GET) {
            return handleGraphQLInfo(request);
        }

        if (request.getHttpMethod() != HttpMethod.POST) {
            return createErrorResponse(request, "GraphQL solo acepta POST requests", 405);
        }

        String body = request.getBody().orElse("{}");
        context.getLogger().info("GraphQL Query recibido: " + body);

        try {
            Map<String, Object> requestMap = objectMapper.readValue(body, Map.class);
            String query = (String) requestMap.get("query");
            Map<String, Object> variables = (Map<String, Object>) requestMap.get("variables");

            if (query == null || query.isEmpty()) {
                return createErrorResponse(request, "Query GraphQL requerido", 400);
            }

            ExecutionResult executionResult = graphQL.execute(query);

            Map<String, Object> response = new HashMap<>();
            response.put("data", executionResult.getData());

            if (!executionResult.getErrors().isEmpty()) {
                response.put("errors", executionResult.getErrors());
            }

            return createSuccessResponse(request, response);

        } catch (Exception e) {
            context.getLogger().severe("Error procesando GraphQL: " + e.getMessage());
            return createErrorResponse(request, "Error procesando query GraphQL: " + e.getMessage(), 500);
        }
    }

    private GraphQL createGraphQL() {
        String schema = """
            type Warehouse {
                id: Int!
                nombre: String!
                direccion: String!
                telefono: String
                email: String
                responsable: String
                estado: String!
                capacidad_max: Int!
                productos_count: Int
                ocupacion_porcentaje: Float
            }

            type WarehouseCapacity {
                warehouse_id: Int!
                warehouse_nombre: String!
                capacidad_max: Int!
                stock_total: Int!
                ocupacion_porcentaje: Float!
            }

            type Query {
                warehouses: [Warehouse]
                warehouse(id: Int!): Warehouse
                warehousesByStatus(status: String!): [Warehouse]
                warehouseCapacity(id: Int!): WarehouseCapacity
            }

            type Mutation {
                createWarehouse(nombre: String!, direccion: String!, capacidad_max: Int!): Warehouse
                updateWarehouse(id: Int!, nombre: String, direccion: String, estado: String): Warehouse
                deleteWarehouse(id: Int!): Boolean
                changeWarehouseStatus(id: Int!, status: String!): Warehouse
            }
        """;

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = newRuntimeWiring()
            .type("Query", builder -> builder
                .dataFetcher("warehouses", getAllWarehousesGraphQLFetcher())
                .dataFetcher("warehouse", getWarehouseByIdGraphQLFetcher())
                .dataFetcher("warehousesByStatus", getWarehousesByStatusGraphQLFetcher())
                .dataFetcher("warehouseCapacity", getWarehouseCapacityGraphQLFetcher())
            )
            .type("Mutation", builder -> builder
                .dataFetcher("createWarehouse", createWarehouseGraphQLFetcher())
                .dataFetcher("updateWarehouse", updateWarehouseGraphQLFetcher())
                .dataFetcher("deleteWarehouse", deleteWarehouseGraphQLFetcher())
                .dataFetcher("changeWarehouseStatus", changeWarehouseStatusGraphQLFetcher())
            )
            .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    private DataFetcher<List<Map<String, Object>>> getAllWarehousesGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            List<Map<String, Object>> warehouses = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = """
                    SELECT b.*,
                           COUNT(p.id) as productos_count,
                           COALESCE(SUM(p.stock), 0) as stock_total
                    FROM bodegas b
                    LEFT JOIN productos p ON b.id = p.bodega_id
                    GROUP BY b.id
                    ORDER BY b.id
                """;

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        Map<String, Object> warehouse = mapResultSetToWarehouse(rs);
                        warehouse.put("productos_count", rs.getInt("productos_count"));

                        int stockTotal = rs.getInt("stock_total");
                        int capacidadMax = rs.getInt("capacidad_max");
                        double ocupacion = capacidadMax > 0 ? (stockTotal * 100.0) / capacidadMax : 0;
                        warehouse.put("ocupacion_porcentaje", ocupacion);

                        warehouses.add(warehouse);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error obteniendo bodegas: " + e.getMessage());
            }

            return warehouses;
        };
    }

    private DataFetcher<Map<String, Object>> getWarehouseByIdGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");

            try (Connection conn = getConnection()) {
                String sql = """
                    SELECT b.*,
                           COUNT(p.id) as productos_count,
                           COALESCE(SUM(p.stock), 0) as stock_total
                    FROM bodegas b
                    LEFT JOIN productos p ON b.id = p.bodega_id
                    WHERE b.id = ?
                    GROUP BY b.id
                """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> warehouse = mapResultSetToWarehouse(rs);
                            warehouse.put("productos_count", rs.getInt("productos_count"));

                            int stockTotal = rs.getInt("stock_total");
                            int capacidadMax = rs.getInt("capacidad_max");
                            double ocupacion = capacidadMax > 0 ? (stockTotal * 100.0) / capacidadMax : 0;
                            warehouse.put("ocupacion_porcentaje", ocupacion);

                            return warehouse;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error obteniendo bodega: " + e.getMessage());
            }

            return null;
        };
    }

    private DataFetcher<List<Map<String, Object>>> getWarehousesByStatusGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            String status = dataFetchingEnvironment.getArgument("status");
            List<Map<String, Object>> warehouses = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM bodegas WHERE estado = ? ORDER BY id";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, status);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            warehouses.add(mapResultSetToWarehouse(rs));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error obteniendo bodegas por estado: " + e.getMessage());
            }

            return warehouses;
        };
    }

    private DataFetcher<Map<String, Object>> getWarehouseCapacityGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");

            try (Connection conn = getConnection()) {
                String sql = """
                    SELECT b.id, b.nombre, b.capacidad_max,
                           COALESCE(SUM(p.stock), 0) as stock_total
                    FROM bodegas b
                    LEFT JOIN productos p ON b.id = p.bodega_id
                    WHERE b.id = ?
                    GROUP BY b.id, b.nombre, b.capacidad_max
                """;

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            Map<String, Object> capacity = new HashMap<>();
                            capacity.put("warehouse_id", rs.getInt("id"));
                            capacity.put("warehouse_nombre", rs.getString("nombre"));
                            capacity.put("capacidad_max", rs.getInt("capacidad_max"));
                            capacity.put("stock_total", rs.getInt("stock_total"));

                            int capacidadMax = rs.getInt("capacidad_max");
                            int stockTotal = rs.getInt("stock_total");
                            double ocupacion = capacidadMax > 0 ? (stockTotal * 100.0) / capacidadMax : 0;
                            capacity.put("ocupacion_porcentaje", ocupacion);

                            return capacity;
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error obteniendo capacidad de bodega: " + e.getMessage());
            }

            return null;
        };
    }

    private DataFetcher<Map<String, Object>> createWarehouseGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            String nombre = dataFetchingEnvironment.getArgument("nombre");
            String direccion = dataFetchingEnvironment.getArgument("direccion");
            Integer capacidadMax = dataFetchingEnvironment.getArgument("capacidad_max");

            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO bodegas (nombre, direccion, telefono, email, responsable, estado, capacidad_max) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, nombre);
                    stmt.setString(2, direccion);
                    stmt.setString(3, "123456789"); // telefono por defecto
                    stmt.setString(4, "bodega@empresa.com"); // email por defecto
                    stmt.setString(5, "Sistema"); // responsable por defecto
                    stmt.setString(6, "ACTIVO");
                    stmt.setInt(7, capacidadMax);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToWarehouse(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error creando bodega: " + e.getMessage());
            }

            return null;
        };
    }

    private DataFetcher<Map<String, Object>> updateWarehouseGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");
            String nombre = dataFetchingEnvironment.getArgument("nombre");
            String direccion = dataFetchingEnvironment.getArgument("direccion");
            String estado = dataFetchingEnvironment.getArgument("estado");

            try (Connection conn = getConnection()) {
                StringBuilder sql = new StringBuilder("UPDATE bodegas SET modificado_en = CURRENT_TIMESTAMP");
                List<Object> params = new ArrayList<>();

                if (nombre != null) {
                    sql.append(", nombre = ?");
                    params.add(nombre);
                }
                if (direccion != null) {
                    sql.append(", direccion = ?");
                    params.add(direccion);
                }
                if (estado != null) {
                    sql.append(", estado = ?");
                    params.add(estado);
                }

                sql.append(" WHERE id = ? RETURNING *");
                params.add(id);

                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToWarehouse(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error actualizando bodega: " + e.getMessage());
            }

            return null;
        };
    }

    private DataFetcher<Boolean> deleteWarehouseGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");

            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM bodegas WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error eliminando bodega: " + e.getMessage());
            }
        };
    }

    private DataFetcher<Map<String, Object>> changeWarehouseStatusGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");
            String status = dataFetchingEnvironment.getArgument("status");

            try (Connection conn = getConnection()) {
                String sql = "UPDATE bodegas SET estado = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ? RETURNING *";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, status);
                    stmt.setInt(2, id);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToWarehouse(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error cambiando estado de bodega: " + e.getMessage());
            }

            return null;
        };
    }

    private HttpResponseMessage handleGraphQLInfo(HttpRequestMessage<Optional<String>> request) {
        Map<String, Object> info = new HashMap<>();
        info.put("endpoint", "/api/WarehouseFunction/graphql");
        info.put("method", "POST");
        info.put("description", "GraphQL endpoint para bodegas");
        info.put("queries", Arrays.asList(
            "warehouses - Listar todas las bodegas con métricas",
            "warehouse(id: Int!) - Obtener bodega por ID",
            "warehousesByStatus(status: String!) - Bodegas por estado",
            "warehouseCapacity(id: Int!) - Capacidad y ocupación de bodega"
        ));
        info.put("mutations", Arrays.asList(
            "createWarehouse(nombre: String!, direccion: String!, capacidad_max: Int!) - Crear bodega",
            "updateWarehouse(id: Int!, nombre: String, direccion: String, estado: String) - Actualizar bodega",
            "deleteWarehouse(id: Int!) - Eliminar bodega",
            "changeWarehouseStatus(id: Int!, status: String!) - Cambiar estado"
        ));
        info.put("example_query", "{ warehouses { id nombre direccion estado capacidad_max ocupacion_porcentaje } }");

        return createSuccessResponse(request, info);
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private Connection getConnection() throws SQLException {
        try {
            DriverManager.setLoginTimeout(15);
            System.out.println("Intentando conectar a: " + DB_URL);
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("Error conectando a PostgreSQL: " + e.getMessage());
            throw new SQLException("Error conectando a PostgreSQL: " + e.getMessage(), e);
        }
    }

    private HttpResponseMessage createSuccessResponse(HttpRequestMessage<Optional<String>> request, Object data) {
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .header("Access-Control-Allow-Origin", "*")
                .body(data)
                .build();
    }

    private HttpResponseMessage createErrorResponse(HttpRequestMessage<Optional<String>> request, String message, int statusCode) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", new Date());
        error.put("status", statusCode);

        return request.createResponseBuilder(HttpStatus.valueOf(statusCode))
                .header("Content-Type", "application/json")
                .header("Access-Control-Allow-Origin", "*")
                .body(error)
                .build();
    }

    private Map<String, Object> mapResultSetToWarehouse(ResultSet rs) throws SQLException {
        Map<String, Object> warehouse = new HashMap<>();
        warehouse.put("id", rs.getInt("id"));
        warehouse.put("nombre", rs.getString("nombre"));
        warehouse.put("direccion", rs.getString("direccion"));
        warehouse.put("telefono", rs.getString("telefono"));
        warehouse.put("email", rs.getString("email"));
        warehouse.put("responsable", rs.getString("responsable"));
        warehouse.put("estado", rs.getString("estado"));
        warehouse.put("capacidad_max", rs.getInt("capacidad_max"));

        // Campos opcionales que podrían no estar en todas las consultas
        try {
            warehouse.put("creado_en", rs.getTimestamp("creado_en"));
            warehouse.put("modificado_en", rs.getTimestamp("modificado_en"));
        } catch (SQLException e) {
            // Estos campos pueden no estar en todas las consultas
        }

        return warehouse;
    }
}