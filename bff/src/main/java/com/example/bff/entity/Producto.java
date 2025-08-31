package com.example.bff.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PRODUCTOS")
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "SKU", nullable = false, unique = true, length = 50)
    private String sku;
    
    @Column(name = "NOMBRE", nullable = false, length = 120)
    private String nombre;
    
    @Column(name = "DESCRIPCION", length = 500)
    private String descripcion;
    
    @Column(name = "STOCK", nullable = false)
    private Integer stock = 0;
    
    @Column(name = "STOCK_MINIMO")
    private Integer stockMinimo = 0;
    
    @Column(name = "STOCK_MAXIMO")
    private Integer stockMaximo;
    
    @Column(name = "PRECIO", precision = 10, scale = 2)
    private BigDecimal precio = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CATEGORIA_ID")
    private Categoria categoria;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BODEGA_ID")
    private Bodega bodega;
    
    @Column(name = "ESTADO", length = 20)
    private String estado = "ACTIVO";
    
    @Column(name = "UNIDAD_MEDIDA", length = 20)
    private String unidadMedida = "UNIDAD";
    
    @Column(name = "PESO", precision = 8, scale = 2)
    private BigDecimal peso;
    
    @Column(name = "DIMENSIONES", length = 50)
    private String dimensiones;
    
    @Column(name = "CREADO_EN")
    private LocalDateTime creadoEn;
    
    @Column(name = "MODIFICADO_EN")
    private LocalDateTime modificadoEn;
    
    @Column(name = "CREADO_POR", length = 50)
    private String creadoPor = "SYSTEM";
    
    @Column(name = "MODIFICADO_POR", length = 50)
    private String modificadoPor = "SYSTEM";
    
    // Constructores
    public Producto() {}
    
    public Producto(String sku, String nombre, String descripcion, Integer stock, BigDecimal precio) {
        this.sku = sku;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stock = stock;
        this.precio = precio;
        this.creadoEn = LocalDateTime.now();
        this.modificadoEn = LocalDateTime.now();
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer stockMinimo) { this.stockMinimo = stockMinimo; }
    
    public Integer getStockMaximo() { return stockMaximo; }
    public void setStockMaximo(Integer stockMaximo) { this.stockMaximo = stockMaximo; }
    
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    
    public Bodega getBodega() { return bodega; }
    public void setBodega(Bodega bodega) { this.bodega = bodega; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getUnidadMedida() { return unidadMedida; }
    public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }
    
    public BigDecimal getPeso() { return peso; }
    public void setPeso(BigDecimal peso) { this.peso = peso; }
    
    public String getDimensiones() { return dimensiones; }
    public void setDimensiones(String dimensiones) { this.dimensiones = dimensiones; }
    
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    
    public LocalDateTime getModificadoEn() { return modificadoEn; }
    public void setModificadoEn(LocalDateTime modificadoEn) { this.modificadoEn = modificadoEn; }
    
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }
    
    public String getModificadoPor() { return modificadoPor; }
    public void setModificadoPor(String modificadoPor) { this.modificadoPor = modificadoPor; }
    
    @PrePersist
    protected void onCreate() {
        creadoEn = LocalDateTime.now();
        modificadoEn = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        modificadoEn = LocalDateTime.now();
    }
}