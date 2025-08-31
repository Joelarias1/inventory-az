package com.example.bff.api;

import com.example.bff.dto.Models.Product;
import com.example.bff.dto.Models.Warehouse;
import com.example.bff.dto.Models.IdRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BffController {

  private final WebClient productFunc;
  private final WebClient warehouseFunc;


  public BffController(
      @Qualifier("productFunc") WebClient productFunc,
      @Qualifier("warehouseFunc") WebClient warehouseFunc
  ) {
    this.productFunc = productFunc;
    this.warehouseFunc = warehouseFunc;
  }

  // Endpoint de health check simple
  @GetMapping("/health")
  public Mono<String> health() {
    return Mono.just("BFF is running! Database connection will be tested when functions are ready.");
  }

  // Endpoint demo para mostrar datos mock (para demostración cuando las functions no responden)
  @GetMapping("/demo/productos")
  public Mono<Map<String, Object>> demoProducts() {
    List<Map<String, Object>> productos = List.of(
        Map.of("id", 1, "sku", "LAP-001", "nombre", "Laptop HP Pavilion", 
               "descripcion", "Laptop 15 pulgadas, 8GB RAM, 256GB SSD",
               "stock", 10, "precio", 599990, "categoria_id", 1, "bodega_id", 1),
        Map.of("id", 2, "sku", "CEL-001", "nombre", "Smartphone Samsung Galaxy",
               "descripcion", "Smartphone 6.1 pulgadas, 128GB", 
               "stock", 25, "precio", 299990, "categoria_id", 1, "bodega_id", 1),
        Map.of("id", 3, "sku", "CAM-001", "nombre", "Camiseta Deportiva",
               "descripcion", "Camiseta 100% algodón, talla M",
               "stock", 50, "precio", 15990, "categoria_id", 2, "bodega_id", 1),
        Map.of("id", 4, "sku", "MES-001", "nombre", "Mesa de Centro", 
               "descripcion", "Mesa de centro de madera, 80x40cm",
               "stock", 5, "precio", 89990, "categoria_id", 3, "bodega_id", 1)
    );
    
    Map<String, Object> response = Map.of(
        "success", true,
        "message", "Datos de demostración - Sistema funcionando correctamente",
        "productos", productos,
        "total", productos.size(),
        "source", "bff-demo-data",
        "architecture", "serverless-bff-orchestration"
    );
    
    return Mono.just(response);
  }

  // Endpoint demo para mostrar datos mock de bodegas
  @GetMapping("/demo/bodegas")
  public Mono<Map<String, Object>> demoWarehouses() {
    List<Map<String, Object>> bodegas = List.of(
        Map.of("id", 1, "nombre", "Bodega Principal", 
               "direccion", "Av. Principal 123, Santiago",
               "responsable", "Juan Pérez", "capacidad_max", 10000, "estado", "ACTIVO"),
        Map.of("id", 2, "nombre", "Bodega Secundaria",
               "direccion", "Av. Secundaria 456, Valparaíso", 
               "responsable", "María González", "capacidad_max", 5000, "estado", "ACTIVO")
    );
    
    Map<String, Object> response = Map.of(
        "success", true,
        "message", "Datos de demostración - Sistema funcionando correctamente", 
        "bodegas", bodegas,
        "total", bodegas.size(),
        "source", "bff-demo-data",
        "architecture", "serverless-bff-orchestration"
    );
    
    return Mono.just(response);
  }

  /* ===================== Productos ===================== */
  @PostMapping("/productos")
  public Mono<Product> createProduct(@Valid @RequestBody Product body) {
    return productFunc.post()
        .uri("/ProductFunction")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Product.class);
  }

  @GetMapping("/productos")
  public Mono<List> listProducts() {
    return productFunc.get()
        .uri("/ProductFunction")
        .retrieve().bodyToMono(List.class);
  }

  @GetMapping("/productos/{id}")
  public Mono<Product> getProduct(@PathVariable Long id) {
    return productFunc.get()
        .uri("/ProductFunction?id=" + id)
        .retrieve().bodyToMono(Product.class);
  }

  @PutMapping("/productos/{id}")
  public Mono<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product body) {
    body.id = id;
    return productFunc.put()
        .uri("/ProductFunction?id=" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Product.class);
  }

  @DeleteMapping("/productos/{id}")
  public Mono<Void> deleteProduct(@PathVariable Long id) {
    return productFunc.delete()
        .uri("/ProductFunction?id=" + id)
        .retrieve().bodyToMono(Void.class);
  }

  /* ===================== Bodegas ===================== */
  @PostMapping("/bodegas")
  public Mono<Warehouse> createWarehouse(@Valid @RequestBody Warehouse body) {
    return warehouseFunc.post()
        .uri("/WarehouseFunction")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @GetMapping("/bodegas")
  public Mono<List> listWarehouses() {
    return warehouseFunc.get()
        .uri("/WarehouseFunction")
        .retrieve().bodyToMono(List.class);
  }

  @GetMapping("/bodegas/{id}")
  public Mono<Warehouse> getWarehouse(@PathVariable Long id) {
    return warehouseFunc.get()
        .uri("/WarehouseFunction?id=" + id)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @PutMapping("/bodegas/{id}")
  public Mono<Warehouse> updateWarehouse(@PathVariable Long id, @Valid @RequestBody Warehouse body) {
    body.id = id;
    return warehouseFunc.put()
        .uri("/WarehouseFunction?id=" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @DeleteMapping("/bodegas/{id}")
  public Mono<Void> deleteWarehouse(@PathVariable Long id) {
    return warehouseFunc.delete()
        .uri("/WarehouseFunction?id=" + id)
        .retrieve().bodyToMono(Void.class);
  }
}
