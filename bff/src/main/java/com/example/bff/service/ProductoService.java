package com.example.bff.service;

import com.example.bff.entity.Producto;
import com.example.bff.entity.Categoria;
import com.example.bff.entity.Bodega;
import com.example.bff.repository.ProductoRepository;
import com.example.bff.repository.CategoriaRepository;
import com.example.bff.repository.BodegaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductoService {
    
    @Autowired
    private ProductoRepository productoRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    @Autowired
    private BodegaRepository bodegaRepository;
    
    public List<Producto> findAll() {
        return productoRepository.findAll();
    }
    
    public List<Producto> findAllActive() {
        return productoRepository.findAllActive();
    }
    
    public Optional<Producto> findById(Long id) {
        return productoRepository.findById(id);
    }
    
    public Optional<Producto> findBySku(String sku) {
        return productoRepository.findBySku(sku);
    }
    
    public List<Producto> findByCategoria(Long categoriaId) {
        Optional<Categoria> categoria = categoriaRepository.findById(categoriaId);
        return categoria.map(productoRepository::findByCategoria).orElse(List.of());
    }
    
    public List<Producto> findByBodega(Long bodegaId) {
        Optional<Bodega> bodega = bodegaRepository.findById(bodegaId);
        return bodega.map(productoRepository::findByBodega).orElse(List.of());
    }
    
    public List<Producto> findProductosConStockBajo() {
        return productoRepository.findProductosConStockBajo();
    }
    
    public Producto save(Producto producto) {
        if (producto.getId() == null && productoRepository.existsBySku(producto.getSku())) {
            throw new RuntimeException("Ya existe un producto con el SKU: " + producto.getSku());
        }
        return productoRepository.save(producto);
    }
    
    public Producto update(Long id, Producto producto) {
        Optional<Producto> existingOpt = productoRepository.findById(id);
        if (existingOpt.isPresent()) {
            Producto existing = existingOpt.get();
            
            // Verificar SKU único solo si cambió
            if (!existing.getSku().equals(producto.getSku()) && 
                productoRepository.existsBySku(producto.getSku())) {
                throw new RuntimeException("Ya existe un producto con el SKU: " + producto.getSku());
            }
            
            // Actualizar campos
            existing.setSku(producto.getSku());
            existing.setNombre(producto.getNombre());
            existing.setDescripcion(producto.getDescripcion());
            existing.setStock(producto.getStock());
            existing.setStockMinimo(producto.getStockMinimo());
            existing.setStockMaximo(producto.getStockMaximo());
            existing.setPrecio(producto.getPrecio());
            existing.setEstado(producto.getEstado());
            existing.setUnidadMedida(producto.getUnidadMedida());
            existing.setPeso(producto.getPeso());
            existing.setDimensiones(producto.getDimensiones());
            
            // Actualizar relaciones si se proporcionaron
            if (producto.getCategoria() != null) {
                existing.setCategoria(producto.getCategoria());
            }
            if (producto.getBodega() != null) {
                existing.setBodega(producto.getBodega());
            }
            
            return productoRepository.save(existing);
        } else {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
    }
    
    public void deleteById(Long id) {
        if (productoRepository.existsById(id)) {
            productoRepository.deleteById(id);
        } else {
            throw new RuntimeException("Producto no encontrado con ID: " + id);
        }
    }
    
    public boolean existsById(Long id) {
        return productoRepository.existsById(id);
    }
}