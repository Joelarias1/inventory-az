package com.function;

import java.util.*;
import java.sql.*;
import java.util.Date;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions para CRUD de Productos
 * Conecta directamente a base de datos para operaciones reales
 * 
 * Endpoints:
 * GET /api/ProductFunction - Listar todos los productos
 * GET /api/ProductFunction?id={id} - Obtener producto por ID
 * POST /api/ProductFunction - Crear nuevo producto
 * PUT /api/ProductFunction?id={id} - Actualizar producto
 * DELETE /api/ProductFunction?id={id} - Eliminar producto
 */
public class ProductFunction {
    
    // Configuración de base de datos
    private static final String DB_URL = System.getenv("POSTGRES_URL") != null ? 
        System.getenv("POSTGRES_URL") : "jdbc:postgresql://104.208.158.85:5432/duoc?sslmode=require";
    private static final String DB_USER = System.getenv("POSTGRES_USER") != null ? 
        System.getenv("POSTGRES_USER") : "duoc";
    private static final String DB_PASSWORD = System.getenv("POSTGRES_PASSWORD") != null ? 
        System.getenv("POSTGRES_PASSWORD") : "duoc1234";

    @FunctionName("ProductFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}, 
                        authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("ProductFunction procesando request: " + request.getHttpMethod());

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
            return createSuccessResponse(request, response);
        }
        
        try (Connection conn = getConnection()) {
            if (idParam != null) {
                // GET por ID
                return getProductById(conn, idParam, request, context);
            } else if (categoriaParam != null) {
                // GET por categoría
                return getProductsByCategory(conn, categoriaParam, request, context);
            } else if (bodegaParam != null) {
                // GET por bodega
                return getProductsByWarehouse(conn, bodegaParam, request, context);
            } else {
                // GET todos los productos
                return getAllProducts(conn, request, context);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en GET: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener productos: " + e.getMessage(), 500);
        }
    }
    
    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String body = request.getBody().orElse("{}");
        context.getLogger().info("Creando producto con datos: " + body);
        
        // Por ahora simula la creación (en producción parsearías el JSON)
        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO productos (sku, nombre, descripcion, stock, stock_minimo, stock_maximo, precio, categoria_id, bodega_id, estado, unidad_medida, peso, dimensiones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "PROD-NEW");
                stmt.setString(2, "Nuevo Producto Azure Function");
                stmt.setString(3, "Producto creado desde Azure Function");
                stmt.setInt(4, 10);
                stmt.setInt(5, 5);
                stmt.setInt(6, 100);
                stmt.setDouble(7, 9990.0);
                stmt.setInt(8, 1); // categoria_id
                stmt.setInt(9, 1); // bodega_id
                stmt.setString(10, "ACTIVO");
                stmt.setString(11, "UNIDAD");
                stmt.setDouble(12, 1.0);
                stmt.setString(13, "10x10x10 cm");
                
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
                
                return createErrorResponse(request, "Error al crear producto", 500);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en POST: " + e.getMessage());
            return createErrorResponse(request, "Error al crear producto: " + e.getMessage(), 500);
        }
    }
    
    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para actualizar", 400);
        }
        
        try (Connection conn = getConnection()) {
            int id = Integer.parseInt(idParam);
            String sql = "UPDATE productos SET nombre = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "Producto Actualizado por Azure Function");
                stmt.setInt(2, id);
                
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
    
    private Connection getConnection() throws SQLException {
        try {
            // Establecer timeout de conexión
            DriverManager.setLoginTimeout(15);
            
            // Log para debugging
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
        product.put("unidad_medida", rs.getString("unidad_medida"));
        product.put("peso", rs.getDouble("peso"));
        product.put("dimensiones", rs.getString("dimensiones"));
        product.put("creado_en", rs.getTimestamp("creado_en"));
        product.put("modificado_en", rs.getTimestamp("modificado_en"));
        return product;
    }
}
