package com.example.bff.repository;

import com.example.bff.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    List<Categoria> findByEstado(String estado);
    
    @Query("SELECT c FROM Categoria c WHERE c.estado = 'ACTIVO'")
    List<Categoria> findAllActive();
    
    boolean existsByNombre(String nombre);
}