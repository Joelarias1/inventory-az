package com.function;

import java.util.*;
import java.sql.*;
import java.util.Date;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Azure Functions para gestión de Inventario
 * API REST para operaciones de control de inventario y movimientos de stock
 *
 * Endpoints:
 * GET /api/InventoryFunction - Listar inventario actual
 * GET /api/InventoryFunction?producto_id={id} - Obtener stock de un producto
 * GET /api/InventoryFunction?bodega_id={id} - Obtener inventario de una bodega
 * POST /api/InventoryFunction/movement - Registrar movimiento de inventario
 * PUT /api/InventoryFunction/adjust - Ajustar stock manualmente
 * GET /api/InventoryFunction/alerts - Obtener alertas de stock bajo
 */
public class InventoryFunction {

    private static final String DB_URL = System.getenv("POSTGRES_URL") != null ?
        System.getenv("POSTGRES_URL") : "jdbc:postgresql://104.208.158.85:5432/duoc?sslmode=require";
    private static final String DB_USER = System.getenv("POSTGRES_USER") != null ?
        System.getenv("POSTGRES_USER") : "duoc";
    private static final String DB_PASSWORD = System.getenv("POSTGRES_PASSWORD") != null ?
        System.getenv("POSTGRES_PASSWORD") : "duoc1234";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("InventoryFunction")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req",
                        methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT},
                        authLevel = AuthorizationLevel.ANONYMOUS,
                        route = "inventory/{action=list}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("action") String action,
            final ExecutionContext context) {

        context.getLogger().info("InventoryFunction procesando: " + request.getHttpMethod() + " - Action: " + action);

        try {
            switch (action.toLowerCase()) {
                case "list":
                    return handleListInventory(request, context);
                case "movement":
                    return handleMovement(request, context);
                case "adjust":
                    return handleAdjustStock(request, context);
                case "alerts":
                    return handleStockAlerts(request, context);
                case "report":
                    return handleInventoryReport(request, context);
                default:
                    return handleListInventory(request, context);
            }
        } catch (Exception e) {
            context.getLogger().severe("Error en InventoryFunction: " + e.getMessage());
            return createErrorResponse(request, "Error interno: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleListInventory(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String productoId = request.getQueryParameters().get("producto_id");
        String bodegaId = request.getQueryParameters().get("bodega_id");
        String categoria = request.getQueryParameters().get("categoria");

        try (Connection conn = getConnection()) {
            StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.sku, p.nombre, p.stock, p.stock_minimo, p.stock_maximo, " +
                "p.precio, p.categoria_id, p.bodega_id, p.estado, " +
                "c.nombre as categoria_nombre, b.nombre as bodega_nombre " +
                "FROM productos p " +
                "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                "LEFT JOIN bodegas b ON p.bodega_id = b.id " +
                "WHERE 1=1"
            );

            List<Object> params = new ArrayList<>();

            if (productoId != null) {
                sql.append(" AND p.id = ?");
                params.add(Integer.parseInt(productoId));
            }
            if (bodegaId != null) {
                sql.append(" AND p.bodega_id = ?");
                params.add(Integer.parseInt(bodegaId));
            }
            if (categoria != null) {
                sql.append(" AND p.categoria_id = ?");
                params.add(Integer.parseInt(categoria));
            }

            sql.append(" ORDER BY p.id");

            List<Map<String, Object>> inventory = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rs.getInt("id"));
                        item.put("sku", rs.getString("sku"));
                        item.put("nombre", rs.getString("nombre"));
                        item.put("stock", rs.getInt("stock"));
                        item.put("stock_minimo", rs.getInt("stock_minimo"));
                        item.put("stock_maximo", rs.getInt("stock_maximo"));
                        item.put("precio", rs.getDouble("precio"));
                        item.put("categoria_id", rs.getInt("categoria_id"));
                        item.put("categoria_nombre", rs.getString("categoria_nombre"));
                        item.put("bodega_id", rs.getInt("bodega_id"));
                        item.put("bodega_nombre", rs.getString("bodega_nombre"));
                        item.put("estado", rs.getString("estado"));

                        // Calcular estado del stock
                        int stock = rs.getInt("stock");
                        int stockMinimo = rs.getInt("stock_minimo");
                        int stockMaximo = rs.getInt("stock_maximo");

                        String stockStatus;
                        if (stock <= 0) {
                            stockStatus = "SIN_STOCK";
                        } else if (stock <= stockMinimo) {
                            stockStatus = "STOCK_BAJO";
                        } else if (stock >= stockMaximo) {
                            stockStatus = "STOCK_ALTO";
                        } else {
                            stockStatus = "STOCK_NORMAL";
                        }
                        item.put("stock_status", stockStatus);

                        inventory.add(item);
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", inventory);
            response.put("total", inventory.size());
            response.put("message", "Inventario obtenido exitosamente");
            response.put("timestamp", new Date());

            return createSuccessResponse(request, response);

        } catch (Exception e) {
            context.getLogger().severe("Error obteniendo inventario: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener inventario: " + e.getMessage(), 500);
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handleMovement(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        if (request.getHttpMethod() != HttpMethod.POST) {
            return createErrorResponse(request, "Método no permitido para movimientos", 405);
        }

        String body = request.getBody().orElse("{}");
        context.getLogger().info("Registrando movimiento de inventario: " + body);

        try {
            Map<String, Object> movementData = objectMapper.readValue(body, Map.class);

            int productoId = ((Number) movementData.get("producto_id")).intValue();
            String tipoMovimiento = (String) movementData.get("tipo_movimiento"); // ENTRADA, SALIDA, AJUSTE
            int cantidad = ((Number) movementData.get("cantidad")).intValue();
            String motivo = (String) movementData.getOrDefault("motivo", "");
            String usuario = (String) movementData.getOrDefault("usuario", "Sistema");

            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);

                try {
                    // Obtener stock actual
                    String selectSql = "SELECT stock FROM productos WHERE id = ? FOR UPDATE";
                    int stockActual = 0;

                    try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                        selectStmt.setInt(1, productoId);
                        try (ResultSet rs = selectStmt.executeQuery()) {
                            if (rs.next()) {
                                stockActual = rs.getInt("stock");
                            } else {
                                conn.rollback();
                                return createErrorResponse(request, "Producto no encontrado", 404);
                            }
                        }
                    }

                    // Calcular nuevo stock
                    int nuevoStock = stockActual;
                    if ("ENTRADA".equalsIgnoreCase(tipoMovimiento)) {
                        nuevoStock += cantidad;
                    } else if ("SALIDA".equalsIgnoreCase(tipoMovimiento)) {
                        nuevoStock -= cantidad;
                        if (nuevoStock < 0) {
                            conn.rollback();
                            return createErrorResponse(request, "Stock insuficiente", 400);
                        }
                    } else if ("AJUSTE".equalsIgnoreCase(tipoMovimiento)) {
                        nuevoStock = cantidad;
                    }

                    // Actualizar stock del producto
                    String updateSql = "UPDATE productos SET stock = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, nuevoStock);
                        updateStmt.setInt(2, productoId);
                        updateStmt.executeUpdate();
                    }

                    // Registrar el movimiento (si tuviéramos tabla de movimientos)
                    // En este caso, solo actualizamos el stock

                    conn.commit();

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", Map.of(
                        "producto_id", productoId,
                        "tipo_movimiento", tipoMovimiento,
                        "cantidad", cantidad,
                        "stock_anterior", stockActual,
                        "stock_nuevo", nuevoStock,
                        "motivo", motivo,
                        "usuario", usuario
                    ));
                    response.put("message", "Movimiento de inventario registrado exitosamente");
                    response.put("timestamp", new Date());

                    return createSuccessResponse(request, response);

                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                }
            }

        } catch (Exception e) {
            context.getLogger().severe("Error en movimiento de inventario: " + e.getMessage());
            return createErrorResponse(request, "Error al registrar movimiento: " + e.getMessage(), 500);
        }
    }

    @SuppressWarnings("unchecked")
    private HttpResponseMessage handleAdjustStock(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        if (request.getHttpMethod() != HttpMethod.PUT) {
            return createErrorResponse(request, "Método no permitido para ajustes", 405);
        }

        String body = request.getBody().orElse("{}");

        try {
            Map<String, Object> adjustData = objectMapper.readValue(body, Map.class);
            int productoId = ((Number) adjustData.get("producto_id")).intValue();
            int nuevoStock = ((Number) adjustData.get("nuevo_stock")).intValue();
            String motivo = (String) adjustData.getOrDefault("motivo", "Ajuste manual");

            try (Connection conn = getConnection()) {
                String sql = "UPDATE productos SET stock = ?, modificado_en = CURRENT_TIMESTAMP WHERE id = ?";

                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, nuevoStock);
                    stmt.setInt(2, productoId);

                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("data", Map.of(
                            "producto_id", productoId,
                            "nuevo_stock", nuevoStock,
                            "motivo", motivo
                        ));
                        response.put("message", "Stock ajustado exitosamente");
                        response.put("timestamp", new Date());

                        return createSuccessResponse(request, response);
                    } else {
                        return createErrorResponse(request, "Producto no encontrado", 404);
                    }
                }
            }

        } catch (Exception e) {
            context.getLogger().severe("Error ajustando stock: " + e.getMessage());
            return createErrorResponse(request, "Error al ajustar stock: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleStockAlerts(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        try (Connection conn = getConnection()) {
            String sql = "SELECT p.id, p.sku, p.nombre, p.stock, p.stock_minimo, p.stock_maximo, " +
                        "c.nombre as categoria_nombre, b.nombre as bodega_nombre " +
                        "FROM productos p " +
                        "LEFT JOIN categorias c ON p.categoria_id = c.id " +
                        "LEFT JOIN bodegas b ON p.bodega_id = b.id " +
                        "WHERE p.stock <= p.stock_minimo OR p.stock >= p.stock_maximo " +
                        "ORDER BY p.stock ASC";

            List<Map<String, Object>> alerts = new ArrayList<>();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("id", rs.getInt("id"));
                    alert.put("sku", rs.getString("sku"));
                    alert.put("nombre", rs.getString("nombre"));
                    alert.put("stock", rs.getInt("stock"));
                    alert.put("stock_minimo", rs.getInt("stock_minimo"));
                    alert.put("stock_maximo", rs.getInt("stock_maximo"));
                    alert.put("categoria", rs.getString("categoria_nombre"));
                    alert.put("bodega", rs.getString("bodega_nombre"));

                    int stock = rs.getInt("stock");
                    int stockMinimo = rs.getInt("stock_minimo");
                    int stockMaximo = rs.getInt("stock_maximo");

                    if (stock <= 0) {
                        alert.put("tipo_alerta", "SIN_STOCK");
                        alert.put("severidad", "CRITICA");
                    } else if (stock <= stockMinimo) {
                        alert.put("tipo_alerta", "STOCK_BAJO");
                        alert.put("severidad", "ALTA");
                    } else if (stock >= stockMaximo) {
                        alert.put("tipo_alerta", "STOCK_EXCESIVO");
                        alert.put("severidad", "MEDIA");
                    }

                    alerts.add(alert);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", alerts);
            response.put("total_alertas", alerts.size());
            response.put("message", "Alertas de stock obtenidas exitosamente");
            response.put("timestamp", new Date());

            return createSuccessResponse(request, response);

        } catch (Exception e) {
            context.getLogger().severe("Error obteniendo alertas: " + e.getMessage());
            return createErrorResponse(request, "Error al obtener alertas: " + e.getMessage(), 500);
        }
    }

    private HttpResponseMessage handleInventoryReport(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        try (Connection conn = getConnection()) {
            // Reporte general del inventario
            Map<String, Object> report = new HashMap<>();

            // Total de productos
            String countSql = "SELECT COUNT(*) as total_productos, " +
                             "SUM(stock * precio) as valor_total_inventario, " +
                             "SUM(stock) as total_unidades " +
                             "FROM productos WHERE estado = 'ACTIVO'";

            try (PreparedStatement stmt = conn.prepareStatement(countSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.put("total_productos", rs.getInt("total_productos"));
                    report.put("valor_total_inventario", rs.getDouble("valor_total_inventario"));
                    report.put("total_unidades", rs.getInt("total_unidades"));
                }
            }

            // Productos con stock bajo
            String lowStockSql = "SELECT COUNT(*) as productos_stock_bajo FROM productos WHERE stock <= stock_minimo";
            try (PreparedStatement stmt = conn.prepareStatement(lowStockSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.put("productos_stock_bajo", rs.getInt("productos_stock_bajo"));
                }
            }

            // Productos sin stock
            String noStockSql = "SELECT COUNT(*) as productos_sin_stock FROM productos WHERE stock = 0";
            try (PreparedStatement stmt = conn.prepareStatement(noStockSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    report.put("productos_sin_stock", rs.getInt("productos_sin_stock"));
                }
            }

            // Top 5 productos con más stock
            String topStockSql = "SELECT nombre, stock, precio, (stock * precio) as valor_total " +
                                "FROM productos WHERE estado = 'ACTIVO' " +
                                "ORDER BY stock DESC LIMIT 5";
            List<Map<String, Object>> topStock = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(topStockSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("nombre", rs.getString("nombre"));
                    item.put("stock", rs.getInt("stock"));
                    item.put("precio", rs.getDouble("precio"));
                    item.put("valor_total", rs.getDouble("valor_total"));
                    topStock.add(item);
                }
            }
            report.put("top_productos_stock", topStock);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", report);
            response.put("message", "Reporte de inventario generado exitosamente");
            response.put("timestamp", new Date());

            return createSuccessResponse(request, response);

        } catch (Exception e) {
            context.getLogger().severe("Error generando reporte: " + e.getMessage());
            return createErrorResponse(request, "Error al generar reporte: " + e.getMessage(), 500);
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