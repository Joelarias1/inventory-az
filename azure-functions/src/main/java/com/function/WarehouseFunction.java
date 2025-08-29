package com.function;

import java.util.*;
import java.sql.*;
import java.util.Date;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions para CRUD de Bodegas (Warehouses)           
 * Conecta directamente a Oracle Cloud para operaciones reales
 * 
 * Endpoints:
 * GET /api/WarehouseFunction - Listar todas las bodegas (desde Oracle)
 * GET /api/WarehouseFunction?id={id} - Obtener bodega por ID (desde Oracle)
 * POST /api/WarehouseFunction - Crear nueva bodega (en Oracle)
 * PUT /api/WarehouseFunction?id={id} - Actualizar bodega (en Oracle)
 * DELETE /api/WarehouseFunction?id={id} - Eliminar bodega (en Oracle)
 */
public class WarehouseFunction {
    
    // Configuración de Oracle Cloud
    private static final String DB_URL = "jdbc:oracle:thin:@inventariobd_high";
    private static final String DB_USER = "admin";
    private static final String DB_PASSWORD = "Inventariobd123!";

    @FunctionName("WarehouseFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}, 
                        authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("WarehouseFunction procesando request: " + request.getHttpMethod());

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
    
    private HttpResponseMessage handleGet(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        
        try (Connection conn = getConnection()) {
            if (idParam != null) {
                // GET por ID
                return getWarehouseById(conn, idParam, request, context);
            } else {
                // GET todas las bodegas
                return getAllWarehouses(conn, request, context);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en GET: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener bodegas: " + e.getMessage(), 500);
        }
    }
    
    private HttpResponseMessage getAllWarehouses(Connection conn, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        String sql = "SELECT ID, NOMBRE, DIRECCION, TELEFONO, EMAIL, RESPONSABLE, ESTADO, CAPACIDAD_MAX, CREADO_EN, MODIFICADO_EN FROM BODEGAS ORDER BY ID";
        
        List<Map<String, Object>> warehouses = new ArrayList<>();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> warehouse = new HashMap<>();
                warehouse.put("id", rs.getInt("ID"));
                warehouse.put("nombre", rs.getString("NOMBRE"));
                warehouse.put("direccion", rs.getString("DIRECCION"));
                warehouse.put("telefono", rs.getString("TELEFONO"));
                warehouse.put("email", rs.getString("EMAIL"));
                warehouse.put("responsable", rs.getString("RESPONSABLE"));
                warehouse.put("estado", rs.getString("ESTADO"));
                warehouse.put("capacidad_max", rs.getInt("CAPACIDAD_MAX"));
                warehouse.put("creado_en", rs.getTimestamp("CREADO_EN"));
                warehouse.put("modificado_en", rs.getTimestamp("MODIFICADO_EN"));
                warehouses.add(warehouse);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", warehouses);
        response.put("total", warehouses.size());
        response.put("message", "Bodegas obtenidas desde Oracle Cloud");
        response.put("timestamp", new Date());
        
        return createSuccessResponse(request, response);
    }
    
    private HttpResponseMessage getWarehouseById(Connection conn, String idParam, HttpRequestMessage<Optional<String>> request, ExecutionContext context) throws SQLException {
        try {
            int id = Integer.parseInt(idParam);
            String sql = "SELECT ID, NOMBRE, DIRECCION, TELEFONO, EMAIL, RESPONSABLE, ESTADO, CAPACIDAD_MAX, CREADO_EN, MODIFICADO_EN FROM BODEGAS WHERE ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> warehouse = new HashMap<>();
                        warehouse.put("id", rs.getInt("ID"));
                        warehouse.put("nombre", rs.getString("NOMBRE"));
                        warehouse.put("direccion", rs.getString("DIRECCION"));
                        warehouse.put("telefono", rs.getString("TELEFONO"));
                        warehouse.put("email", rs.getString("EMAIL"));
                        warehouse.put("responsable", rs.getString("RESPONSABLE"));
                        warehouse.put("estado", rs.getString("ESTADO"));
                        warehouse.put("capacidad_max", rs.getInt("CAPACIDAD_MAX"));
                        warehouse.put("creado_en", rs.getTimestamp("CREADO_EN"));
                        warehouse.put("modificado_en", rs.getTimestamp("MODIFICADO_EN"));
                        
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", warehouse);
                        response.put("message", "Bodega encontrada en Oracle Cloud");
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
    
    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String body = request.getBody().orElse("{}");
        context.getLogger().info("Creando bodega con datos: " + body);
        
        // Por ahora simula la creación (en producción parsearías el JSON)
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO BODEGAS (NOMBRE, DIRECCION, TELEFONO, EMAIL, RESPONSABLE, ESTADO, CAPACIDAD_MAX) VALUES (?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, "Nueva Bodega Azure Function");
                stmt.setString(2, "Dirección por defecto");
                stmt.setString(3, "123456789");
                stmt.setString(4, "bodega@empresa.com");
                stmt.setString(5, "Sistema Azure Function");
                stmt.setString(6, "ACTIVO");
                stmt.setInt(7, 1000);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    // Obtener el ID generado
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int newId = rs.getInt(1);
                            
                            Map<String, Object> response = new HashMap<>();
                            response.put("success", true);
                            response.put("data", Map.of("id", newId, "message", "Bodega creada en Oracle Cloud"));
                            response.put("message", "Bodega creada exitosamente en Oracle Cloud");
                            response.put("timestamp", new Date());
                            
                            return createSuccessResponse(request, response);
                        }
                    }
                }
                
                return createErrorResponse(request, "Error al crear bodega", 500);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en POST: " + e.getMessage());
            return createErrorResponse(request, "Error al crear bodega: " + e.getMessage(), 500);
        }
    }
    
    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para actualizar", 400);
        }
        
        try (Connection conn = getConnection()) {
            int id = Integer.parseInt(idParam);
            String sql = "UPDATE BODEGAS SET NOMBRE = ?, MODIFICADO_EN = SYSTIMESTAMP WHERE ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "Bodega Actualizada por Azure Function");
                stmt.setInt(2, id);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of("id", id, "message", "Bodega actualizada en Oracle Cloud"));
                    response.put("message", "Bodega actualizada exitosamente en Oracle Cloud");
                    response.put("timestamp", new Date());
                    
                    return createSuccessResponse(request, response);
                } else {
                    return createErrorResponse(request, "Bodega no encontrada", 404);
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
            String sql = "DELETE FROM BODEGAS WHERE ID = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of("id", id, "message", "Bodega eliminada de Oracle Cloud"));
                    response.put("message", "Bodega eliminada exitosamente de Oracle Cloud");
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
    
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    private HttpResponseMessage createSuccessResponse(HttpRequestMessage<Optional<String>> request, Object data) {
        return request.createResponseBuilder(HttpStatus.OK)
                .header("Content-Type", "application/json")
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
                .body(error)
                .build();
    }
}
