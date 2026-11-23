package com.tottalstore.sifen.facturacion.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Documentos FIRMADOS del mismo tipo a enviar en un lote (siRecepLoteDE: hasta 50). */
public record LoteRequest(
        @NotEmpty(message = "Elegí al menos un documento")
        @Size(max = 50, message = "Un lote lleva hasta 50 documentos")
        List<UUID> facturaIds) {
}
