package com.tottalstore.sifen.registro;

import com.tottalstore.sifen.registro.dto.CatalogoRg90Response;
import com.tottalstore.sifen.registro.dto.ComprobanteRegistradoRequest;
import com.tottalstore.sifen.registro.dto.ComprobanteRegistradoResponse;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
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

/** Registro de compras y de ventas no electrónicas (RG 90/2021). Borrar es solo ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/comprobantes-registrados")
public class ComprobanteRegistradoController {

    private final ComprobanteRegistradoService service;

    public ComprobanteRegistradoController(ComprobanteRegistradoService service) {
        this.service = service;
    }

    @GetMapping
    public List<ComprobanteRegistradoResponse> listar(
            @RequestParam YearMonth periodo, @RequestParam(required = false) TipoRegistro tipo) {
        return service.listar(periodo, tipo).stream().map(ComprobanteRegistradoResponse::from).toList();
    }

    @GetMapping("/catalogo")
    public CatalogoRg90Response catalogo() {
        return CatalogoRg90Response.crear();
    }

    @GetMapping("/{id}")
    public ComprobanteRegistradoResponse obtener(@PathVariable UUID id) {
        return ComprobanteRegistradoResponse.from(service.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComprobanteRegistradoResponse crear(@Valid @RequestBody ComprobanteRegistradoRequest request) {
        return ComprobanteRegistradoResponse.from(service.crear(request));
    }

    @PutMapping("/{id}")
    public ComprobanteRegistradoResponse editar(
            @PathVariable UUID id, @Valid @RequestBody ComprobanteRegistradoRequest request) {
        return ComprobanteRegistradoResponse.from(service.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        service.eliminar(id);
    }
}
