package com.example.bff.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "BODEGAS")
public class Bodega {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    
    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;
    
    @Column(name = "DIRECCION", length = 255)
    private String direccion;
    
    @Column(name = "TELEFONO", length = 20)
    private String telefono;
    
    @Column(name = "EMAIL", length = 100)
    private String email;
    
    @Column(name = "RESPONSABLE", length = 100)
    private String responsable;
    
    @Column(name = "ESTADO", length = 20)
    private String estado = "ACTIVO";
    
    @Column(name = "CAPACIDAD_MAX")
    private Integer capacidadMax = 0;
    
    @Column(name = "CREADO_EN")
    private LocalDateTime creadoEn;
    
    @Column(name = "MODIFICADO_EN")
    private LocalDateTime modificadoEn;
    
    @Column(name = "CREADO_POR", length = 50)
    private String creadoPor = "SYSTEM";
    
    @Column(name = "MODIFICADO_POR", length = 50)
    private String modificadoPor = "SYSTEM";
    
    // Constructores
    public Bodega() {}
    
    public Bodega(String nombre, String direccion, String responsable, Integer capacidadMax) {
        this.nombre = nombre;
        this.direccion = direccion;
        this.responsable = responsable;
        this.capacidadMax = capacidadMax;
        this.creadoEn = LocalDateTime.now();
        this.modificadoEn = LocalDateTime.now();
    }
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public Integer getCapacidadMax() { return capacidadMax; }
    public void setCapacidadMax(Integer capacidadMax) { this.capacidadMax = capacidadMax; }
    
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