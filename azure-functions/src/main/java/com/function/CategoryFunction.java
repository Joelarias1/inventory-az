package com.function;

import java.util.*;
import java.sql.*;
import java.util.Date;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Azure Functions para CRUD de Categorías
 * API REST para operaciones con categorías de productos
 *
 * Endpoints:
 * GET /api/CategoryFunction - Listar todas las categorías
 * GET /api/CategoryFunction?id={id} - Obtener categoría por ID
 * POST /api/CategoryFunction - Crear nueva categoría
 * PUT /api/CategoryFunction?id={id} - Actualizar categoría
 * DELETE /api/CategoryFunction?id={id} - Eliminar categoría
 */
public class CategoryFunction {

    private static final String DB_URL = System.getenv("POSTGRES_URL") != null ?
        System.getenv("POSTGRES_URL") : "jdbc:postgresql://104.208.158.85:5432/duoc?sslmode=require";
    private static final String DB_USER = System.getenv("POSTGRES_USER") != null ?
        System.getenv("POSTGRES_USER") : "duoc";
    private static final String DB_PASSWORD = System.getenv("POSTGRES_PASSWORD") != null ?
        System.getenv("POSTGRES_PASSWORD") : "duoc1234";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("CategoryFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req",
                        methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE},
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "categories")
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("CategoryFunction procesando request: " + request.getHttpMethod());

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
            context.getLogger().severe("Error en CategoryFunction: " + e.getMessage());
            return createErrorResponse(request, "Error interno: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");

        try (Connection conn = getConnection()) {
            if (idParam != null) {
                return getCategoryById(conn, idParam, request, context);
            } else {
                return getAllCategories(conn, request, context);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en GET: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener categorías: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage getAllCategories(Connection conn, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        String sql = "SELECT id, nombre, descripcion, estado, creado_en, modificado_en FROM categorias ORDER BY id";

        List<Map<String, Object>> categories = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> category = new HashMap<>();
                category.put("id", rs.getInt("id"));
                category.put("nombre", rs.getString("nombre"));
                category.put("descripcion", rs.getString("descripcion"));
                category.put("estado", rs.getString("estado"));
                category.put("creado_en", rs.getTimestamp("creado_en"));
                category.put("modificado_en", rs.getTimestamp("modificado_en"));
                categories.add(category);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", categories);
        response.put("total", categories.size());
        response.put("message", "Categorías obtenidas exitosamente");
        response.put("timestamp", new Date());

        return createSuccessResponse(request, response);
    }

    private HttpResponseMessage getCategoryById(Connection conn, String idParam, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        try {
            int id = Integer.parseInt(idParam);
            String sql = "SELECT id, nombre, descripcion, estado, creado_en, modificado_en FROM categorias WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> category = new HashMap<>();
                        category.put("id", rs.getInt("id"));
                        category.put("nombre", rs.getString("nombre"));
                        category.put("descripcion", rs.getString("descripcion"));
                        category.put("estado", rs.getString("estado"));
                        category.put("creado_en", rs.getTimestamp("creado_en"));
                        category.put("modificado_en", rs.getTimestamp("modificado_en"));

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", category);
                        response.put("message", "Categoría encontrada exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Categoría no encontrada", 404);
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
        context.getLogger().info("Creando categoría con datos: " + body);

        try {
            Map<String, Object> categoryData = objectMapper.readValue(body, Map.class);

            try (Connection conn = getConnection()) {
                String sql = "INSERT INTO categorias (nombre, descripcion, estado) VALUES (?, ?, ?) RETURNING id";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) categoryData.getOrDefault("nombre", "Nueva Categoría"));
                    stmt.setString(2, (String) categoryData.getOrDefault("descripcion", "Descripción de categoría"));
                    stmt.setString(3, (String) categoryData.getOrDefault("estado", "ACTIVO"));

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int newId = rs.getInt("id");

                            Map<String, Object> response = new HashMap<>();
                            response.put("success", true);
                            response.put("data", Map.of(
                                "id", newId,
                                "nombre", categoryData.getOrDefault("nombre", "Nueva Categoría"),
                                "message", "Categoría creada exitosamente"
                            ));
                            response.put("message", "Categoría creada exitosamente");
                            response.put("timestamp", new Date());

                            return createSuccessResponse(request, response);
                        }
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en POST: " + e.getMessage());
            return createErrorResponse(request, "Error al crear categoría: " + e.getMessage(), 500);
        }

        return createErrorResponse(request, "Error al crear categoría", 500);
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para actualizar", 400);
        }

        String body = request.getBody().orElse("{}");

        try {
            Map<String, Object> categoryData = objectMapper.readValue(body, Map.class);
            int id = Integer.parseInt(idParam);

            try (Connection conn = getConnection()) {
                String sql = "UPDATE categorias SET nombre = ?, descripcion = ?, estado = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, (String) categoryData.getOrDefault("nombre", "Categoría Actualizada"));
                    stmt.setString(2, (String) categoryData.getOrDefault("descripcion", "Descripción actualizada"));
                    stmt.setString(3, (String) categoryData.getOrDefault("estado", "ACTIVO"));
                    stmt.setInt(4, id);

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", Map.of("id", id, "message", "Categoría actualizada exitosamente"));
                        response.put("message", "Categoría actualizada exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Categoría no encontrada", 404);
                    }
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en PUT: " + e.getMessage());
            return createErrorResponse(request, "Error al actualizar categoría: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para eliminar", 400);
        }

        try (Connection conn = getConnection()) {
            int id = Integer.parseInt(idParam);
            String sql = "DELETE FROM categorias WHERE id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of("id", id, "message", "Categoría eliminada exitosamente"));
                    response.put("message", "Categoría eliminada exitosamente");
                    response.put("timestamp", new Date());

                    return createSuccessResponse(request, response);
                } else {
                    return createErrorResponse(request, "Categoría no encontrada", 404);
                }
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en DELETE: " + e.getMessage());
            return createErrorResponse(request, "Error al eliminar categoría: " + e.getMessage(), 500);
        }
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
}