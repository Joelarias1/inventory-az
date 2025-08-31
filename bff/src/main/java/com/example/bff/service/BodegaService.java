package com.example.bff.service;

import com.example.bff.entity.Bodega;
import com.example.bff.repository.BodegaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BodegaService {
    
    @Autowired
    private BodegaRepository bodegaRepository;
    
    public List<Bodega> findAll() {
        return bodegaRepository.findAll();
    }
    
    public List<Bodega> findAllActive() {
        return bodegaRepository.findAllActive();
    }
    
    public Optional<Bodega> findById(Long id) {
        return bodegaRepository.findById(id);
    }
    
    public List<Bodega> findByResponsable(String responsable) {
        return bodegaRepository.findByResponsable(responsable);
    }
    
    public List<Bodega> findByCapacidadMinima(Integer capacidad) {
        return bodegaRepository.findByCapacidadMinima(capacidad);
    }
    
    public Bodega save(Bodega bodega) {
        return bodegaRepository.save(bodega);
    }
    
    public Bodega update(Long id, Bodega bodega) {
        Optional<Bodega> existingOpt = bodegaRepository.findById(id);
        if (existingOpt.isPresent()) {
            Bodega existing = existingOpt.get();
            
            // Actualizar campos
            existing.setNombre(bodega.getNombre());
            existing.setDireccion(bodega.getDireccion());
            existing.setTelefono(bodega.getTelefono());
            existing.setEmail(bodega.getEmail());
            existing.setResponsable(bodega.getResponsable());
            existing.setEstado(bodega.getEstado());
            existing.setCapacidadMax(bodega.getCapacidadMax());
            
            return bodegaRepository.save(existing);
        } else {
            throw new RuntimeException("Bodega no encontrada con ID: " + id);
        }
    }
    
    public void deleteById(Long id) {
        if (bodegaRepository.existsById(id)) {
            bodegaRepository.deleteById(id);
        } else {
            throw new RuntimeException("Bodega no encontrada con ID: " + id);
        }
    }
    
    public boolean existsById(Long id) {
        return bodegaRepository.existsById(id);
    }
}