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
import graphql.ExecutionInput;
import graphql.schema.DataFetcher;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;

/**
 * Azure Functions para CRUD de Productos con soporte REST y GraphQL
 *
 * Endpoints REST:
 * GET /api/ProductFunction - Listar todos los productos
 * GET /api/ProductFunction?id={id} - Obtener producto por ID
 * POST /api/ProductFunction - Crear nuevo producto
 * PUT /api/ProductFunction?id={id} - Actualizar producto
 * DELETE /api/ProductFunction?id={id} - Eliminar producto
 *
 * Endpoint GraphQL:
 * POST /api/ProductFunction/graphql - Endpoint GraphQL
 */
public class ProductFunction {

    // Configuración de base de datos
    private static final String DB_URL = System.getenv("POSTGRES_URL") != null ?
        System.getenv("POSTGRES_URL") : "jdbc:postgresql://104.208.158.85:5432/duoc?sslmode=require";
    private static final String DB_USER = System.getenv("POSTGRES_USER") != null ?
        System.getenv("POSTGRES_USER") : "duoc";
    private static final String DB_PASSWORD = System.getenv("POSTGRES_PASSWORD") != null ?
        System.getenv("POSTGRES_PASSWORD") : "duoc1234";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GraphQL graphQL;

    public ProductFunction() {
        this.graphQL = createGraphQL();
    }

    @FunctionName("ProductFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req",
                        methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "ProductFunction/{action=rest}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("action") String action,
            final ExecutionContext context) {

        context.getLogger().info("ProductFunction procesando request: " + request.getHttpMethod() + " - Action: " + action);

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
            context.getLogger().severe("Error en ProductFunction: " + e.getMessage());
            return createErrorResponse(request, "Error interno: " + e.getMessage(), 500);
        }
    }

    // ==================== MÉTODOS REST ====================

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        String categoriaParam = request.getQueryParameters().get("categoria");
        String bodegaParam = request.getQueryParameters().get("bodega");
        String testParam = request.getQueryParameters().get("test");

        // Endpoint de prueba sin BD
        if (testParam != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ProductFunction funcionando correctamente");
            response.put("timestamp", new Date());
            response.put("test", true);
            response.put("supports", Arrays.asList("REST", "GraphQL"));
            return createSuccessResponse(request, response);
        }

        try (Connection conn = getConnection()) {
            if (idParam != null) {
                return getProductById(conn, idParam, request, context);
            } else if (categoriaParam != null) {
                return getProductsByCategory(conn, categoriaParam, request, context);
            } else if (bodegaParam != null) {
                return getProductsByWarehouse(conn, bodegaParam, request, context);
            } else {
                return getAllProducts(conn, request, context);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en GET: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener productos: " + e.getMessage(), 500);
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String body = request.getBody().orElse("{}");
        context.getLogger().info("Creando producto con datos: " + body);

        try {
            Map<String, Object> productData = objectMapper.readValue(body, Map.class);

            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO productos (sku, nombre, descripcion, stock, stock_minimo, stock_maximo, precio, categoria_id, bodega_id, estado, unidad_medida, peso, dimensiones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) productData.getOrDefault("sku", "PROD-NEW"));
                    stmt.setString(2, (String) productData.getOrDefault("nombre", "Nuevo Producto"));
                    stmt.setString(3, (String) productData.getOrDefault("descripcion", "Descripción del producto"));
                    stmt.setInt(4, ((Number) productData.getOrDefault("stock", 10)).intValue());
                    stmt.setInt(5, ((Number) productData.getOrDefault("stock_minimo", 5)).intValue());
                    stmt.setInt(6, ((Number) productData.getOrDefault("stock_maximo", 100)).intValue());
                    stmt.setDouble(7, ((Number) productData.getOrDefault("precio", 9990.0)).doubleValue());
                    stmt.setInt(8, ((Number) productData.getOrDefault("categoria_id", 1)).intValue());
                    stmt.setInt(9, ((Number) productData.getOrDefault("bodega_id", 1)).intValue());
                    stmt.setString(10, (String) productData.getOrDefault("estado", "ACTIVO"));
                    stmt.setString(11, (String) productData.getOrDefault("unidad_medida", "UNIDAD"));
                    stmt.setDouble(12, ((Number) productData.getOrDefault("peso", 1.0)).doubleValue());
                    stmt.setString(13, (String) productData.getOrDefault("dimensiones", "10x10x10 cm"));

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int newId = rs.getInt("id");

                            Map<String, Object> response = new HashMap<>();
                            response.put("success", true);
                            response.put("data", Map.of("id", newId, "message", "Producto creado exitosamente"));
                            response.put("message", "Producto creado exitosamente");
                            response.put("timestamp", new Date());

                            return createSuccessResponse(request, response);
                        }
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en POST: " + e.getMessage());
            return createErrorResponse(request, "Error al crear producto: " + e.getMessage(), 500);
        }

        return createErrorResponse(request, "Error al crear producto", 500);
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para actualizar", 400);
        }

        String body = request.getBody().orElse("{}");

        try {
            Map<String, Object> productData = objectMapper.readValue(body, Map.class);
            int id = Integer.parseInt(idParam);

            try (Connection conn = getConnection()) {
                String sql = "UPDATE productos SET nombre = ?, descripcion = ?, stock = ?, precio = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) productData.getOrDefault("nombre", "Producto Actualizado"));
                    stmt.setString(2, (String) productData.getOrDefault("descripcion", "Descripción actualizada"));
                    stmt.setInt(3, ((Number) productData.getOrDefault("stock", 10)).intValue());
                    stmt.setDouble(4, ((Number) productData.getOrDefault("precio", 9990.0)).doubleValue());
                    stmt.setInt(5, id);

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", Map.of("id", id, "message", "Producto actualizado exitosamente"));
                        response.put("message", "Producto actualizado exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Producto no encontrado", 404);
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en PUT: " + e.getMessage());
            return createErrorResponse(request, "Error al actualizar producto: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para eliminar", 400);
        }

        try (Connection conn = getConnection()) {
            int id = Integer.parseInt(idParam);
            String sql = "DELETE FROM productos WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of("id", id, "message", "Producto eliminado exitosamente"));
                    response.put("message", "Producto eliminado exitosamente");
                    response.put("timestamp", new Date());

                    return createSuccessResponse(request, response);
                } else {
                    return createErrorResponse(request, "Producto no encontrado", 404);
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en DELETE: " + e.getMessage());
            return createErrorResponse(request, "Error al eliminar producto: " + e.getMessage(), 500);
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

            // CONSTRUIR LA EJECUCIÓN CON VARIABLES
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables != null ? variables : Collections.emptyMap())
                .build();

            ExecutionResult executionResult = graphQL.execute(executionInput);

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
            type Product {
                id: Int!
                sku: String!
                nombre: String!
                descripcion: String
                stock: Int!
                stock_minimo: Int!
                stock_maximo: Int!
                precio: Float!
                categoria_id: Int!
                bodega_id: Int!
                estado: String!
            }

            type Query {
                products: [Product]
                product(id: Int!): Product
                productsByCategory(categoryId: Int!): [Product]
                productsByWarehouse(warehouseId: Int!): [Product]
            }

            type Mutation {
                createProduct(sku: String!, nombre: String!, stock: Int!, precio: Float!): Product
                updateProduct(id: Int!, nombre: String, stock: Int, precio: Float): Product
                deleteProduct(id: Int!): Boolean
            }
        """;

        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schema);

        RuntimeWiring runtimeWiring = newRuntimeWiring()
            .type("Query", builder -> builder
                .dataFetcher("products", getAllProductsGraphQLFetcher())
                .dataFetcher("product", getProductByIdGraphQLFetcher())
                .dataFetcher("productsByCategory", getProductsByCategoryGraphQLFetcher())
                .dataFetcher("productsByWarehouse", getProductsByWarehouseGraphQLFetcher())
            )
            .type("Mutation", builder -> builder
                .dataFetcher("createProduct", createProductGraphQLFetcher())
                .dataFetcher("updateProduct", updateProductGraphQLFetcher())
                .dataFetcher("deleteProduct", deleteProductGraphQLFetcher())
            )
            .build();

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

        return GraphQL.newGraphQL(graphQLSchema).build();
    }

    private DataFetcher<List<Map<String, Object>>> getAllProductsGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            List<Map<String, Object>> products = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM productos ORDER BY id";

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    while (rs.next()) {
                        products.add(mapResultSetToProduct(rs));
                    }
                }
            }

            return products;
        };
    }

    private DataFetcher<Map<String, Object>> getProductByIdGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");

            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM productos WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToProduct(rs);
                        }
                    }
                }
            }

            return null;
        };
    }

    private DataFetcher<List<Map<String, Object>>> getProductsByCategoryGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer categoryId = dataFetchingEnvironment.getArgument("categoryId");
            List<Map<String, Object>> products = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM productos WHERE categoria_id = ? ORDER BY id";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, categoryId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            products.add(mapResultSetToProduct(rs));
                        }
                    }
                }
            }

            return products;
        };
    }

    private DataFetcher<List<Map<String, Object>>> getProductsByWarehouseGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer warehouseId = dataFetchingEnvironment.getArgument("warehouseId");
            List<Map<String, Object>> products = new ArrayList<>();

            try (Connection conn = getConnection()) {
                String sql = "SELECT * FROM productos WHERE bodega_id = ? ORDER BY id";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, warehouseId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            products.add(mapResultSetToProduct(rs));
                        }
                    }
                }
            }

            return products;
        };
    }

    private DataFetcher<Map<String, Object>> createProductGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            String sku = dataFetchingEnvironment.getArgument("sku");
            String nombre = dataFetchingEnvironment.getArgument("nombre");
            Integer stock = dataFetchingEnvironment.getArgument("stock");
            Double precio = dataFetchingEnvironment.getArgument("precio");

            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO productos (sku, nombre, stock, precio, stock_minimo, stock_maximo, categoria_id, bodega_id, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, sku);
                    stmt.setString(2, nombre);
                    stmt.setInt(3, stock);
                    stmt.setDouble(4, precio);
                    stmt.setInt(5, 5); // stock_minimo por defecto
                    stmt.setInt(6, 100); // stock_maximo por defecto
                    stmt.setInt(7, 1); // categoria_id por defecto
                    stmt.setInt(8, 1); // bodega_id por defecto
                    stmt.setString(9, "ACTIVO");

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToProduct(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error creando producto: " + e.getMessage());
            }

            return null;
        };
    }

    private DataFetcher<Map<String, Object>> updateProductGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");
            String nombre = dataFetchingEnvironment.getArgument("nombre");
            Integer stock = dataFetchingEnvironment.getArgument("stock");
            Double precio = dataFetchingEnvironment.getArgument("precio");

            try (Connection conn = getConnection()) {
                StringBuilder sql = new StringBuilder("UPDATE productos SET modificado_en = CURRENT_TIMESTAMP");
                List<Object> params = new ArrayList<>();

                if (nombre != null) {
                    sql.append(", nombre = ?");
                    params.add(nombre);
                }
                if (stock != null) {
                    sql.append(", stock = ?");
                    params.add(stock);
                }
                if (precio != null) {
                    sql.append(", precio = ?");
                    params.add(precio);
                }

                sql.append(" WHERE id = ? RETURNING *");
                params.add(id);

                try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                    for (int i = 0; i < params.size(); i++) {
                        stmt.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            return mapResultSetToProduct(rs);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error actualizando producto: " + e.getMessage());
            }

            return null;
        };
    }

    private DataFetcher<Boolean> deleteProductGraphQLFetcher() {
        return dataFetchingEnvironment -> {
            Integer id = dataFetchingEnvironment.getArgument("id");

            try (Connection conn = getConnection()) {
                String sql = "DELETE FROM productos WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    int affectedRows = stmt.executeUpdate();
                    return affectedRows > 0;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error eliminando producto: " + e.getMessage());
            }
        };
    }

    private HttpResponseMessage handleGraphQLInfo(HttpRequestMessage<Optional<String>> request) {
        Map<String, Object> info = new HashMap<>();
        info.put("endpoint", "/api/ProductFunction/graphql");
        info.put("method", "POST");
        info.put("description", "GraphQL endpoint para productos");
        info.put("queries", Arrays.asList(
            "products - Listar todos los productos",
            "product(id: Int!) - Obtener producto por ID",
            "productsByCategory(categoryId: Int!) - Productos por categoría",
            "productsByWarehouse(warehouseId: Int!) - Productos por bodega"
        ));
        info.put("mutations", Arrays.asList(
            "createProduct(sku: String!, nombre: String!, stock: Int!, precio: Float!) - Crear producto",
            "updateProduct(id: Int!, nombre: String, stock: Int, precio: Float) - Actualizar producto",
            "deleteProduct(id: Int!) - Eliminar producto"
        ));
        info.put("example_query", "{ products { id sku nombre stock precio } }");

        return createSuccessResponse(request, info);
    }

    // ==================== MÉTODOS AUXILIARES ====================

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

    private HttpResponseMessage getAllProducts(Connection conn, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        String sql = "SELECT id, sku, nombre, descripcion, stock, stock_minimo, stock_maximo, precio, categoria_id, bodega_id, estado, unidad_medida, peso, dimensiones, creado_en, modificado_en FROM productos ORDER BY id";

        List<Map<String, Object>> products = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> product = mapResultSetToProduct(rs);
                products.add(product);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);
        response.put("total", products.size());
        response.put("message", "Productos obtenidos exitosamente");
        response.put("timestamp", new Date());

        return createSuccessResponse(request, response);
    }

    private HttpResponseMessage getProductById(Connection conn, String idParam, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        try {
            int id = Integer.parseInt(idParam);
            String sql = "SELECT id, sku, nombre, descripcion, stock, stock_minimo, stock_maximo, precio, categoria_id, bodega_id, estado, unidad_medida, peso, dimensiones, creado_en, modificado_en FROM productos WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> product = mapResultSetToProduct(rs);

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", product);
                        response.put("message", "Producto encontrado exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Producto no encontrado", 404);
                    }
                }
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(request, "ID inválido", 400);
        }
    }

    private HttpResponseMessage getProductsByCategory(Connection conn, String categoriaParam, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        try {
            int categoriaId = Integer.parseInt(categoriaParam);
            String sql = "SELECT id, sku, nombre, descripcion, stock, stock_minimo, stock_maximo, precio, categoria_id, bodega_id, estado, unidad_medida, peso, dimensiones, creado_en, modificado_en FROM productos WHERE categoria_id = ? ORDER BY id";

            List<Map<String, Object>> products = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, categoriaId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> product = mapResultSetToProduct(rs);
                        products.add(product);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("total", products.size());
            response.put("categoria_id", categoriaId);
            response.put("message", "Productos obtenidos por categoria exitosamente");
            response.put("timestamp", new Date());

            return createSuccessResponse(request, response);
        } catch (NumberFormatException e) {
            return createErrorResponse(request, "ID de categoría inválido", 400);
        }
    }

    private HttpResponseMessage getProductsByWarehouse(Connection conn, String bodegaParam, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        try {
            int bodegaId = Integer.parseInt(bodegaParam);
            String sql = "SELECT id, sku, nombre, descripcion, stock, stock_minimo, stock_maximo, precio, categoria_id, bodega_id, estado, unidad_medida, peso, dimensiones, creado_en, modificado_en FROM productos WHERE bodega_id = ? ORDER BY id";

            List<Map<String, Object>> products = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, bodegaId);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> product = mapResultSetToProduct(rs);
                        products.add(product);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", products);
            response.put("total", products.size());
            response.put("bodega_id", bodegaId);
            response.put("message", "Productos obtenidos por bodega exitosamente");
            response.put("timestamp", new Date());

            return createSuccessResponse(request, response);
        } catch (NumberFormatException e) {
            return createErrorResponse(request, "ID de bodega inválido", 400);
        }
    }

    private Map<String, Object> mapResultSetToProduct(ResultSet rs) throws SQLException {
        Map<String, Object> product = new HashMap<>();
        product.put("id", rs.getInt("id"));
        product.put("sku", rs.getString("sku"));
        product.put("nombre", rs.getString("nombre"));
        product.put("descripcion", rs.getString("descripcion"));
        product.put("stock", rs.getInt("stock"));
        product.put("stock_minimo", rs.getInt("stock_minimo"));
        product.put("stock_maximo", rs.getInt("stock_maximo"));
        product.put("precio", rs.getDouble("precio"));
        product.put("categoria_id", rs.getInt("categoria_id"));
        product.put("bodega_id", rs.getInt("bodega_id"));
        product.put("estado", rs.getString("estado"));

        // Campos opcionales que podrían no estar en todas las consultas
        try {
            product.put("unidad_medida", rs.getString("unidad_medida"));
            product.put("peso", rs.getDouble("peso"));
            product.put("dimensiones", rs.getString("dimensiones"));
            product.put("creado_en", rs.getTimestamp("creado_en"));
            product.put("modificado_en", rs.getTimestamp("modificado_en"));
        } catch (SQLException e) {
            // Estos campos pueden no estar en todas las consultas
        }

        return product;
    }
}