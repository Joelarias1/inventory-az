package com.example.bff.api;

import com.example.bff.dto.Models.Product;
import com.example.bff.dto.Models.Warehouse;
import com.example.bff.dto.Models.IdRequest;
import com.example.bff.dto.Models.FunctionResponse;
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
      @Qualifier("warehouseFunc") WebClient warehouseFunc) {
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
        .uri("/productfunction")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Product.class);
  }

  @GetMapping("/productos")
  public Mono<List> listProducts() {
    return productFunc.get()
        .uri("/productfunction")
        .retrieve().bodyToMono(FunctionResponse.class)
        .map(response -> (List) response.data);
  }

  @GetMapping("/productos/{id}")
  public Mono<Object> getProduct(@PathVariable Long id) {
    return productFunc.get()
        .uri("/productfunction?id=" + id)
        .retrieve().bodyToMono(FunctionResponse.class)
        .map(response -> response.data);
  }

  @PutMapping("/productos/{id}")
  public Mono<Product> updateProduct(@PathVariable Long id, @Valid @RequestBody Product body) {
    body.id = id;
    return productFunc.put()
        .uri("/productfunction?id=" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Product.class);
  }

  @DeleteMapping("/productos/{id}")
  public Mono<Void> deleteProduct(@PathVariable Long id) {
    return productFunc.delete()
        .uri("/productfunction?id=" + id)
        .retrieve().bodyToMono(Void.class);
  }

  /* ===================== Bodegas ===================== */
  @PostMapping("/bodegas")
  public Mono<Warehouse> createWarehouse(@Valid @RequestBody Warehouse body) {
    return warehouseFunc.post()
        .uri("/warehousefunction")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @GetMapping("/bodegas")
  public Mono<List> listWarehouses() {
    return warehouseFunc.get()
        .uri("/warehousefunction")
        .retrieve().bodyToMono(FunctionResponse.class)
        .map(response -> (List) response.data);
  }

  @GetMapping("/bodegas/{id}")
  public Mono<Object> getWarehouse(@PathVariable Long id) {
    return warehouseFunc.get()
        .uri("/warehousefunction?id=" + id)
        .retrieve().bodyToMono(FunctionResponse.class)
        .map(response -> response.data);
  }

  @PutMapping("/bodegas/{id}")
  public Mono<Warehouse> updateWarehouse(@PathVariable Long id, @Valid @RequestBody Warehouse body) {
    body.id = id;
    return warehouseFunc.put()
        .uri("/warehousefunction?id=" + id)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Warehouse.class);
  }

  @DeleteMapping("/bodegas/{id}")
  public Mono<Void> deleteWarehouse(@PathVariable Long id) {
    return warehouseFunc.delete()
        .uri("/warehousefunction?id=" + id)
        .retrieve().bodyToMono(Void.class);
  }
}
