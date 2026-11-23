package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.facturacion.dto.LoteRequest;
import com.tottalstore.sifen.facturacion.dto.LoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Envío asíncrono por lote al SIFEN (siRecepLoteDE / siResultLoteDE). */
@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @GetMapping
    public List<LoteResponse> listar() {
        return loteService.listar();
    }

    @GetMapping("/{id}")
    public LoteResponse obtener(@PathVariable UUID id) {
        return loteService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponse enviar(@Valid @RequestBody LoteRequest request) {
        return loteService.enviar(request.facturaIds());
    }

    /** siResultLoteDE: mientras el SIFEN responda 0361 (en procesamiento), hay que volver a consultar. */
    @PostMapping("/{id}/consultar")
    public LoteResponse consultar(@PathVariable UUID id) {
        return loteService.consultar(id);
    }
}
