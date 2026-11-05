package com.tottalstore.sifen.configuracion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActividadEconomicaRequest(
        @NotBlank(message = "El código de la actividad económica es obligatorio")
        @Size(max = 10, message = "El código de la actividad económica admite hasta 10 caracteres")
        String codigo,
        @NotBlank(message = "La descripción de la actividad económica es obligatoria")
        @Size(max = 300, message = "La descripción de la actividad económica admite hasta 300 caracteres")
        String descripcion,
        boolean principal) {
}
