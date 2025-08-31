package com.example.bff.api;

import com.example.bff.entity.Producto;
import com.example.bff.entity.Bodega;
import com.example.bff.service.ProductoService;
import com.example.bff.service.BodegaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private BodegaService bodegaService;

    // ============== PRODUCTOS (CRUD con Oracle Database) ==============

    @GetMapping("/productos")
    public ResponseEntity<Map<String, Object>> getAllProductos() {
        try {
            List<Producto> productos = productoService.findAllActive();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            response.put("source", "oracle-database");
            response.put("message", "Productos obtenidos desde Oracle Database");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener productos: " + e.getMessage());
            error.put("source", "oracle-database");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<Map<String, Object>> getProducto(@PathVariable Long id) {
        try {
            Optional<Producto> producto = productoService.findById(id);
            Map<String, Object> response = new HashMap<>();
            
            if (producto.isPresent()) {
                response.put("success", true);
                response.put("data", producto.get());
                response.put("source", "oracle-database");
                response.put("message", "Producto encontrado");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Producto no encontrado");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener producto: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/productos")
    public ResponseEntity<Map<String, Object>> createProducto(@Valid @RequestBody Producto producto) {
        try {
            Producto nuevoProducto = productoService.save(producto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", nuevoProducto);
            response.put("source", "oracle-database");
            response.put("message", "Producto creado exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al crear producto: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<Map<String, Object>> updateProducto(@PathVariable Long id, 
                                                             @Valid @RequestBody Producto producto) {
        try {
            Producto productoActualizado = productoService.update(id, producto);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productoActualizado);
            response.put("source", "oracle-database");
            response.put("message", "Producto actualizado exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al actualizar producto: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/productos/{id}")
    public ResponseEntity<Map<String, Object>> deleteProducto(@PathVariable Long id) {
        try {
            productoService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Producto eliminado exitosamente");
            response.put("source", "oracle-database");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al eliminar producto: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============== BODEGAS (CRUD con Oracle Database) ==============

    @GetMapping("/bodegas")
    public ResponseEntity<Map<String, Object>> getAllBodegas() {
        try {
            List<Bodega> bodegas = bodegaService.findAllActive();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bodegas);
            response.put("total", bodegas.size());
            response.put("source", "oracle-database");
            response.put("message", "Bodegas obtenidas desde Oracle Database");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener bodegas: " + e.getMessage());
            error.put("source", "oracle-database");
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/bodegas/{id}")
    public ResponseEntity<Map<String, Object>> getBodega(@PathVariable Long id) {
        try {
            Optional<Bodega> bodega = bodegaService.findById(id);
            Map<String, Object> response = new HashMap<>();
            
            if (bodega.isPresent()) {
                response.put("success", true);
                response.put("data", bodega.get());
                response.put("source", "oracle-database");
                response.put("message", "Bodega encontrada");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Bodega no encontrada");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener bodega: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/bodegas")
    public ResponseEntity<Map<String, Object>> createBodega(@Valid @RequestBody Bodega bodega) {
        try {
            Bodega nuevaBodega = bodegaService.save(bodega);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", nuevaBodega);
            response.put("source", "oracle-database");
            response.put("message", "Bodega creada exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al crear bodega: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/bodegas/{id}")
    public ResponseEntity<Map<String, Object>> updateBodega(@PathVariable Long id, 
                                                           @Valid @RequestBody Bodega bodega) {
        try {
            Bodega bodegaActualizada = bodegaService.update(id, bodega);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", bodegaActualizada);
            response.put("source", "oracle-database");
            response.put("message", "Bodega actualizada exitosamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al actualizar bodega: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/bodegas/{id}")
    public ResponseEntity<Map<String, Object>> deleteBodega(@PathVariable Long id) {
        try {
            bodegaService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bodega eliminada exitosamente");
            response.put("source", "oracle-database");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al eliminar bodega: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ============== ENDPOINTS ESPECIALES ==============

    @GetMapping("/productos/stock-bajo")
    public ResponseEntity<Map<String, Object>> getProductosStockBajo() {
        try {
            List<Producto> productos = productoService.findProductosConStockBajo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            response.put("source", "oracle-database");
            response.put("message", "Productos con stock bajo");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener productos con stock bajo: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/productos/categoria/{categoriaId}")
    public ResponseEntity<Map<String, Object>> getProductosByCategoria(@PathVariable Long categoriaId) {
        try {
            List<Producto> productos = productoService.findByCategoria(categoriaId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            response.put("categoria_id", categoriaId);
            response.put("source", "oracle-database");
            response.put("message", "Productos por categoría");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener productos por categoría: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/productos/bodega/{bodegaId}")
    public ResponseEntity<Map<String, Object>> getProductosByBodega(@PathVariable Long bodegaId) {
        try {
            List<Producto> productos = productoService.findByBodega(bodegaId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productos);
            response.put("total", productos.size());
            response.put("bodega_id", bodegaId);
            response.put("source", "oracle-database");
            response.put("message", "Productos por bodega");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Error al obtener productos por bodega: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // Health check específico para la base de datos
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // Intenta obtener un producto para verificar conexión
            List<Producto> productos = productoService.findAll();
            List<Bodega> bodegas = bodegaService.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("database", "connected");
            response.put("productos_count", productos.size());
            response.put("bodegas_count", bodegas.size());
            response.put("source", "oracle-database");
            response.put("message", "Conexión a Oracle Database exitosa");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("database", "disconnected");
            error.put("error", "Error de conexión a BD: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(error);
        }
    }
}