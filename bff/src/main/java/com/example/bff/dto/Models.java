package com.example.bff.dto;

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
}
