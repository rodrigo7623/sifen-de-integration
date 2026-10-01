package com.tottalstore.sifen.establecimientos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstablecimientoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "\\d{3}", message = "El código debe tener 3 dígitos (ej. 001)")
        String codigo,
        @NotBlank(message = "La denominación es obligatoria") String denominacion,
        String direccion,
        String numeroCasa,
        String departamentoCodigo,
        String departamentoDescripcion,
        String distritoCodigo,
        String distritoDescripcion,
        String ciudadCodigo,
        String ciudadDescripcion,
        String telefono,
        String email) {
}
