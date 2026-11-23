package com.tottalstore.sifen.facturacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** GEC003 / GEI008 mOtEve: motivo del evento, de 5 a 500 caracteres. */
public record MotivoEventoRequest(
        @NotBlank(message = "Indicá el motivo")
        @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
        String motivo) {
}
