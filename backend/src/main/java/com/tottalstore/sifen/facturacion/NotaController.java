package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.facturacion.dto.FacturaResponse;
import com.tottalstore.sifen.facturacion.dto.NotaRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alta y edición de borradores de notas de crédito/débito (M6). Una vez creada, la nota es un DE
 * más: se confirma, reabre y descarga con los mismos endpoints de {@link FacturaController}.
 */
@RestController
@RequestMapping("/api/notas")
public class NotaController {

    private final FacturaService facturaService;

    public NotaController(FacturaService facturaService) {
        this.facturaService = facturaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FacturaResponse crear(@Valid @RequestBody NotaRequest request) {
        return FacturaResponse.from(facturaService.crearNota(request));
    }

    @PutMapping("/{id}")
    public FacturaResponse editar(@PathVariable UUID id, @Valid @RequestBody NotaRequest request) {
        return FacturaResponse.from(facturaService.editarNota(id, request));
    }
}
