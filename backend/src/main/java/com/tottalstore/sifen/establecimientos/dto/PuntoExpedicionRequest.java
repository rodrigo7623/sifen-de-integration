package com.tottalstore.sifen.establecimientos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PuntoExpedicionRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "\\d{3}", message = "El código debe tener 3 dígitos (ej. 001)")
        String codigo,
        String descripcion) {
}
