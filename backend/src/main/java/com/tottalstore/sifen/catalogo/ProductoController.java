package com.tottalstore.sifen.catalogo;

import com.tottalstore.sifen.catalogo.dto.ProductoRequest;
import com.tottalstore.sifen.catalogo.dto.ProductoResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<ProductoResponse> buscar(
            @RequestParam(required = false) String q,
            @RequestParam(name = "incluirInactivos", defaultValue = "false") boolean incluirInactivos) {
        return productoService.buscar(q, incluirInactivos).stream().map(ProductoResponse::from).toList();
    }

    @GetMapping("/{codigo}")
    public ProductoResponse obtener(@PathVariable String codigo) {
        return ProductoResponse.from(productoService.obtener(codigo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.from(productoService.crear(request));
    }

    @PutMapping("/{codigo}")
    public ProductoResponse editar(@PathVariable String codigo, @Valid @RequestBody ProductoRequest request) {
        return ProductoResponse.from(productoService.editar(codigo, request));
    }

    @DeleteMapping("/{codigo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable String codigo) {
        productoService.desactivar(codigo);
    }
}
