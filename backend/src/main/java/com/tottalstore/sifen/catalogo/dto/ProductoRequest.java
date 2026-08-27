package com.tottalstore.sifen.catalogo.dto;

import com.tottalstore.sifen.shared.TasaIva;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio") String codigo,
        @NotBlank(message = "La descripción es obligatoria") String descripcion,
        @NotBlank(message = "La unidad de medida es obligatoria") String unidadMedida,
        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        BigDecimal precioBase,
        @NotNull(message = "La tasa de IVA es obligatoria") TasaIva tasaIva) {
}
