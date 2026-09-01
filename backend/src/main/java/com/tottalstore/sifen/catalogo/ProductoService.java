package com.tottalstore.sifen.catalogo;

import com.tottalstore.sifen.catalogo.dto.ProductoRequest;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Gestión del catálogo de productos (CU-10). */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> buscar(String q, boolean incluirInactivos) {
        if (q == null || q.isBlank()) {
            return incluirInactivos
                    ? productoRepository.findAllByOrderByDescripcionAsc()
                    : productoRepository.findByActivoTrueOrderByDescripcionAsc();
        }
        return incluirInactivos ? productoRepository.buscarTodos(q.trim()) : productoRepository.buscar(q.trim());
    }

    public Producto obtener(String codigo) {
        return productoRepository.findById(codigo)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: " + codigo));
    }

    public Producto crear(ProductoRequest request) {
        if (productoRepository.existsById(request.codigo())) {
            // Flujo alternativo 4a del CU-10: código duplicado.
            throw new BusinessException("Ya existe un producto con el código " + request.codigo());
        }
        validarPrecio(request.precioBase());

        Producto producto = new Producto();
        aplicar(producto, request);
        return productoRepository.save(producto);
    }

    public Producto editar(String codigo, ProductoRequest request) {
        Producto producto = obtener(codigo);
        validarPrecio(request.precioBase());
        aplicar(producto, request);
        return productoRepository.save(producto);
    }

    public void desactivar(String codigo) {
        Producto producto = obtener(codigo);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private void validarPrecio(BigDecimal precio) {
        // Flujo alternativo 4b del CU-10: precio inválido.
        if (precio == null || precio.signum() <= 0) {
            throw new BusinessException("El precio debe ser mayor a cero");
        }
    }

    private void aplicar(Producto producto, ProductoRequest request) {
        producto.setCodigo(request.codigo());
        producto.setDescripcion(request.descripcion());
        producto.setUnidadMedida(request.unidadMedida());
        producto.setPrecioBase(request.precioBase());
        producto.setTasaIva(request.tasaIva());
    }
}
