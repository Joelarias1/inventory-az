package com.example.bff.api;

import com.example.bff.dto.Models.Product;
import com.example.bff.dto.Models.Warehouse;
import com.example.bff.dto.Models.IdRequest;
import jakarta.validation.Valid;

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

  /* ===================== Productos ===================== */
  @PostMapping("/productos")
  public Mono<Product> createProduct(@Valid @RequestBody Product body) {
    return productFunc.post()
        .uri("/createProduct")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Product.class);
  }

  @GetMapping("/productos")
  public Mono<List> listProducts() {
    return productFunc.get()
        .uri("/listProducts")
        .retrieve().bodyToMono(List.class);
  }

  @GetMapping("/productos/{id}")
  public Mono<Product> getProduct(@PathVariable Long id) {
    IdRequest req = new IdRequest();
    req.id = id;
    return productFunc.post()
        .uri("/getProductById")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve().bodyToMono(Product.class);
  }

  @PutMapping("/productos/{id}")
  public Mono<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product body) {
    body.id = id;
    return productFunc.post()
        .uri("/updateProduct")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Product.class);
  }

  @DeleteMapping("/productos/{id}")
  public Mono<Void> deleteProduct(@PathVariable Long id) {
    IdRequest req = new IdRequest();
    req.id = id;
    return productFunc.post()
        .uri("/deleteProduct")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve().bodyToMono(Void.class);
  }

  /* ===================== Bodegas ===================== */
  @PostMapping("/bodegas")
  public Mono<Warehouse> createWarehouse(@Valid @RequestBody Warehouse body) {
    return warehouseFunc.post()
        .uri("/createWarehouse")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @GetMapping("/bodegas")
  public Mono<List> listWarehouses() {
    return warehouseFunc.get()
        .uri("/listWarehouses")
        .retrieve().bodyToMono(List.class);
  }

  @GetMapping("/bodegas/{id}")
  public Mono<Warehouse> getWarehouse(@PathVariable Long id) {
    IdRequest req = new IdRequest();
    req.id = id;
    return warehouseFunc.post()
        .uri("/getWarehouseById")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @PutMapping("/bodegas/{id}")
  public Mono<Warehouse> updateWarehouse(@PathVariable Long id, @Valid @RequestBody Warehouse body) {
    body.id = id;
    return warehouseFunc.post()
        .uri("/updateWarehouse")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @DeleteMapping("/bodegas/{id}")
  public Mono<Void> deleteWarehouse(@PathVariable Long id) {
    IdRequest req = new IdRequest();
    req.id = id;
    return warehouseFunc.post()
        .uri("/deleteWarehouse")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(req)
        .retrieve().bodyToMono(Void.class);
  }
}
