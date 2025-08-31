package com.example.bff.repository;

import com.example.bff.entity.Bodega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BodegaRepository extends JpaRepository<Bodega, Long> {
    
    List<Bodega> findByEstado(String estado);
    
    @Query("SELECT b FROM Bodega b WHERE b.estado = 'ACTIVO'")
    List<Bodega> findAllActive();
    
    List<Bodega> findByResponsable(String responsable);
    
    @Query("SELECT b FROM Bodega b WHERE b.capacidadMax >= :capacidad")
    List<Bodega> findByCapacidadMinima(Integer capacidad);
}