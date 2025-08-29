package com.function;

import java.util.*;
import java.util.stream.Collectors;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions para CRUD de Productos
 * Endpoints:
 * GET /api/ProductFunction - Listar todos los productos
 * GET /api/ProductFunction?id={id} - Obtener producto por ID
 * POST /api/ProductFunction - Crear nuevo producto
 * PUT /api/ProductFunction?id={id} - Actualizar producto
 * DELETE /api/ProductFunction?id={id} - Eliminar producto
 */
public class ProductFunction {
    
    // Mock data para productos
    private static final List<Map<String, Object>> MOCK_PRODUCTS = new ArrayList<>();
    private static int nextId = 1;
    
    static {
        // Datos iniciales
        addMockProduct("LAP-001", "Laptop HP Pavilion", "Laptop 15 pulgadas, 8GB RAM, 256GB SSD", 10, 2, 599990, 1, 1);
        addMockProduct("CEL-001", "Smartphone Samsung Galaxy", "Smartphone 6.1 pulgadas, 128GB", 25, 5, 299990, 1, 1);
        addMockProduct("CAM-001", "Camiseta Deportiva", "Camiseta 100% algodón, talla M", 50, 10, 15990, 2, 1);
        addMockProduct("MES-001", "Mesa de Centro", "Mesa de centro de madera, 80x40cm", 5, 1, 89990, 3, 1);
    }
    
    private static void addMockProduct(String sku, String nombre, String descripcion, int stock, int stockMinimo, 
                                     double precio, int categoriaId, int bodegaId) {
        Map<String, Object> product = new HashMap<>();
        product.put("id", nextId++);
        product.put("sku", sku);
        product.put("nombre", nombre);
        product.put("descripcion", descripcion);
        product.put("stock", stock);
        product.put("stock_minimo", stockMinimo);
        product.put("stock_maximo", 1000);
        product.put("precio", precio);
        product.put("categoria_id", categoriaId);
        product.put("bodega_id", bodegaId);
        product.put("estado", "ACTIVO");
        product.put("unidad_medida", "UNIDAD");
        product.put("peso", 1.5);
        product.put("dimensiones", "30x20x10 cm");
        product.put("creado_en", new Date());
        product.put("modificado_en", new Date());
        MOCK_PRODUCTS.add(product);
    }

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
        
        if (idParam != null) {
            // GET por ID
            try {
                int id = Integer.parseInt(idParam);
                Optional<Map<String, Object>> product = MOCK_PRODUCTS.stream()
                    .filter(p -> p.get("id").equals(id))
                    .findFirst();
                
                if (product.isPresent()) {
                    return createSuccessResponse(request, product.get());
                } else {
                    return createErrorResponse(request, "Producto no encontrado", 404);
                }
            } catch (NumberFormatException e) {
                return createErrorResponse(request, "ID inválido", 400);
            }
        } else if (categoriaParam != null) {
            // GET por categoría
            try {
                int categoriaId = Integer.parseInt(categoriaParam);
                List<Map<String, Object>> filteredProducts = MOCK_PRODUCTS.stream()
                    .filter(p -> p.get("categoria_id").equals(categoriaId))
                    .collect(Collectors.toList());
                
                Map<String, Object> response = new HashMap<>();
                response.put("productos", filteredProducts);
                response.put("total", filteredProducts.size());
                response.put("categoria_id", categoriaId);
                
                return createSuccessResponse(request, response);
            } catch (NumberFormatException e) {
                return createErrorResponse(request, "ID de categoría inválido", 400);
            }
        } else if (bodegaParam != null) {
            // GET por bodega
            try {
                int bodegaId = Integer.parseInt(bodegaParam);
                List<Map<String, Object>> filteredProducts = MOCK_PRODUCTS.stream()
                    .filter(p -> p.get("bodega_id").equals(bodegaId))
                    .collect(Collectors.toList());
                
                Map<String, Object> response = new HashMap<>();
                response.put("productos", filteredProducts);
                response.put("total", filteredProducts.size());
                response.put("bodega_id", bodegaId);
                
                return createSuccessResponse(request, response);
            } catch (NumberFormatException e) {
                return createErrorResponse(request, "ID de bodega inválido", 400);
            }
        } else {
            // GET todos los productos
            Map<String, Object> response = new HashMap<>();
            response.put("productos", MOCK_PRODUCTS);
            response.put("total", MOCK_PRODUCTS.size());
            response.put("timestamp", new Date());
            
            return createSuccessResponse(request, response);
        }
    }
    
    private HttpResponseMessage handlePost(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String body = request.getBody().orElse("{}");
        context.getLogger().info("Creando producto con datos: " + body);
        
        // Simular creación (en un caso real, parsearías el JSON)
        Map<String, Object> newProduct = new HashMap<>();
        newProduct.put("id", nextId++);
        newProduct.put("sku", "PROD-" + String.format("%03d", nextId - 1));
        newProduct.put("nombre", "Nuevo Producto " + (nextId - 1));
        newProduct.put("descripcion", "Producto creado automáticamente");
        newProduct.put("stock", 0);
        newProduct.put("stock_minimo", 5);
        newProduct.put("stock_maximo", 100);
        newProduct.put("precio", 9990.0);
        newProduct.put("categoria_id", 1);
        newProduct.put("bodega_id", 1);
        newProduct.put("estado", "ACTIVO");
        newProduct.put("unidad_medida", "UNIDAD");
        newProduct.put("peso", 1.0);
        newProduct.put("dimensiones", "10x10x10 cm");
        newProduct.put("creado_en", new Date());
        newProduct.put("modificado_en", new Date());
        
        MOCK_PRODUCTS.add(newProduct);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Producto creado exitosamente");
        response.put("producto", newProduct);
        
        return createSuccessResponse(request, response);
    }
    
    private HttpResponseMessage handlePut(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para actualizar", 400);
        }
        
        try {
            int id = Integer.parseInt(idParam);
            Optional<Map<String, Object>> productOpt = MOCK_PRODUCTS.stream()
                .filter(p -> p.get("id").equals(id))
                .findFirst();
            
            if (productOpt.isPresent()) {
                Map<String, Object> product = productOpt.get();
                product.put("modificado_en", new Date());
                product.put("nombre", product.get("nombre") + " (Actualizado)");
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Producto actualizado exitosamente");
                response.put("producto", product);
                
                return createSuccessResponse(request, response);
            } else {
                return createErrorResponse(request, "Producto no encontrado", 404);
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(request, "ID inválido", 400);
        }
    }
    
    private HttpResponseMessage handleDelete(HttpRequestMessage<Optional<String>> request, ExecutionContext context) {
        String idParam = request.getQueryParameters().get("id");
        if (idParam == null) {
            return createErrorResponse(request, "ID requerido para eliminar", 400);
        }
        
        try {
            int id = Integer.parseInt(idParam);
            boolean removed = MOCK_PRODUCTS.removeIf(p -> p.get("id").equals(id));
            
            if (removed) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "Producto eliminado exitosamente");
                response.put("id", id);
                
                return createSuccessResponse(request, response);
            } else {
                return createErrorResponse(request, "Producto no encontrado", 404);
            }
        } catch (NumberFormatException e) {
            return createErrorResponse(request, "ID inválido", 400);
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
        error.put("error", message);
        error.put("timestamp", new Date());
        error.put("status", statusCode);
        
        return request.createResponseBuilder(HttpStatus.valueOf(statusCode))
                .header("Content-Type", "application/json")
                .body(error)
                .build();
    }
}
