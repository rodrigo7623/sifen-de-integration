package com.tottalstore.sifen.establecimientos;

import com.tottalstore.sifen.establecimientos.dto.EstablecimientoRequest;
import com.tottalstore.sifen.establecimientos.dto.EstablecimientoResponse;
import com.tottalstore.sifen.establecimientos.dto.PuntoExpedicionRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Lectura abierta a ambos roles (la necesita el formulario de factura); altas/bajas/ediciones
 * restringidas a ADMIN en {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/establecimientos")
public class EstablecimientoController {

    private final EstablecimientoService establecimientoService;

    public EstablecimientoController(EstablecimientoService establecimientoService) {
        this.establecimientoService = establecimientoService;
    }

    @GetMapping
    public List<EstablecimientoResponse> listar(
            @RequestParam(name = "incluirInactivos", defaultValue = "false") boolean incluirInactivos) {
        return establecimientoService.listar(incluirInactivos).stream()
                .map(EstablecimientoResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public EstablecimientoResponse obtener(@PathVariable UUID id) {
        return EstablecimientoResponse.from(establecimientoService.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstablecimientoResponse crear(@Valid @RequestBody EstablecimientoRequest request) {
        return EstablecimientoResponse.from(establecimientoService.crear(request));
    }

    @PutMapping("/{id}")
    public EstablecimientoResponse editar(@PathVariable UUID id, @Valid @RequestBody EstablecimientoRequest request) {
        return EstablecimientoResponse.from(establecimientoService.editar(id, request));
    }

    @PostMapping("/{id}/activar")
    public EstablecimientoResponse activar(@PathVariable UUID id) {
        establecimientoService.activar(id);
        return EstablecimientoResponse.from(establecimientoService.obtener(id));
    }

    @PostMapping("/{id}/desactivar")
    public EstablecimientoResponse desactivar(@PathVariable UUID id) {
        establecimientoService.desactivar(id);
        return EstablecimientoResponse.from(establecimientoService.obtener(id));
    }

    @PostMapping("/{id}/puntos-expedicion")
    @ResponseStatus(HttpStatus.CREATED)
    public EstablecimientoResponse agregarPunto(
            @PathVariable UUID id, @Valid @RequestBody PuntoExpedicionRequest request) {
        return EstablecimientoResponse.from(establecimientoService.agregarPuntoExpedicion(id, request));
    }

    @PutMapping("/{id}/puntos-expedicion/{puntoId}")
    public EstablecimientoResponse editarPunto(
            @PathVariable UUID id, @PathVariable UUID puntoId, @Valid @RequestBody PuntoExpedicionRequest request) {
        return EstablecimientoResponse.from(establecimientoService.editarPuntoExpedicion(id, puntoId, request));
    }

    @PostMapping("/{id}/puntos-expedicion/{puntoId}/activar")
    public EstablecimientoResponse activarPunto(@PathVariable UUID id, @PathVariable UUID puntoId) {
        return EstablecimientoResponse.from(
                establecimientoService.cambiarEstadoPuntoExpedicion(id, puntoId, true));
    }

    @PostMapping("/{id}/puntos-expedicion/{puntoId}/desactivar")
    public EstablecimientoResponse desactivarPunto(@PathVariable UUID id, @PathVariable UUID puntoId) {
        return EstablecimientoResponse.from(
                establecimientoService.cambiarEstadoPuntoExpedicion(id, puntoId, false));
    }
}
