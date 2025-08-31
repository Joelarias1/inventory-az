package com.example.bff.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CATEGORIAS")
public class Categoria {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "NOMBRE", nullable = false, length = 50)
    private String nombre;
    
    @Column(name = "DESCRIPCION", length = 255)
    private String descripcion;
    
    @Column(name = "ESTADO", length = 20)
    private String estado = "ACTIVO";
    
    @Column(name = "CREADO_EN")
    private LocalDateTime creadoEn;
    
    @Column(name = "MODIFICADO_EN")
    private LocalDateTime modificadoEn;
    
    @Column(name = "CREADO_POR", length = 50)
    private String creadoPor = "SYSTEM";
    
    @Column(name = "MODIFICADO_POR", length = 50)
    private String modificadoPor = "SYSTEM";
    
    // Constructores
    public Categoria() {}
    
    public Categoria(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.creadoEn = LocalDateTime.now();
        this.modificadoEn = LocalDateTime.now();
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
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