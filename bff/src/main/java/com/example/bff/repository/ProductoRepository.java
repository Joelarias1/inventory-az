package com.example.bff.repository;

import com.example.bff.entity.Producto;
import com.example.bff.entity.Categoria;
import com.example.bff.entity.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    Optional<Producto> findBySku(String sku);
    
    List<Producto> findByEstado(String estado);
    
    @Query("SELECT p FROM Producto p WHERE p.estado = 'ACTIVO'")
    List<Producto> findAllActive();
    
    List<Producto> findByCategoria(Categoria categoria);
    
    List<Producto> findByBodega(Bodega bodega);
    
    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockMinimo")
    List<Producto> findProductosConStockBajo();
    
    @Query("SELECT p FROM Producto p WHERE p.stock = 0")
    List<Producto> findProductosSinStock();
    
    @Query("SELECT p FROM Producto p WHERE p.nombre LIKE %:nombre%")
    List<Producto> findByNombreContaining(@Param("nombre") String nombre);
    
    boolean existsBySku(String sku);
}