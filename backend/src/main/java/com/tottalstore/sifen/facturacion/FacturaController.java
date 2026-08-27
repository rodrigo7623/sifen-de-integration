package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.facturacion.dto.FacturaRequest;
import com.tottalstore.sifen.facturacion.dto.FacturaResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/facturas")
public class FacturaController {

    private final FacturaService facturaService;

    public FacturaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<FacturaResponse> listar(@RequestParam(required = false) EstadoDte estado) {
        return facturaService.listar(estado).stream().map(FacturaResponse::from).toList();
    }

    @GetMapping("/{id}")
    public FacturaResponse obtener(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crearBorrador(@Valid @RequestBody FacturaRequest request) {
        return FacturaResponse.from(facturaService.crearBorrador(request));
    }

    @PutMapping("/{id}")
    public FacturaResponse editarBorrador(@PathVariable UUID id, @Valid @RequestBody FacturaRequest request) {
        return FacturaResponse.from(facturaService.editarBorrador(id, request));
    }

    @PostMapping("/{id}/confirmar")
    public FacturaResponse confirmar(@PathVariable UUID id) {
        return FacturaResponse.from(facturaService.confirmarEnvio(id));
    }
}
