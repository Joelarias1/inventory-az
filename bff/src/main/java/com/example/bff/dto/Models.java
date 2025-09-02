package com.example.bff.dto;

import java.util.List;

public class Models {
  public static class Product {
    public Long id;
    public String sku;
    public String nombre;
    public Integer stock;
    public Long bodegaId;
  }
  public static class Warehouse {
    public Long id;
    public String nombre;
    public String direccion;
  }
  public static class IdRequest {
    public Long id;
  }
  
  public static class FunctionResponse<T> {
    public boolean success;
    public T data;
    public Integer total;
    public String message;
    public String timestamp;
  }
}
