package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.shared.TasaIva;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * {@code productoCodigo} es opcional: si se informa, el ítem proviene del catálogo (RF-12); si no,
 * es un ítem cargado ad-hoc (RF-13) y {@code descripcion}/{@code precioUnitario}/{@code tasaIva}
 * son obligatorios.
 */
public record ItemFacturaRequest(
        String productoCodigo,
        @NotBlank(message = "La descripción del ítem es obligatoria") String descripcion,
        @NotNull(message = "La cantidad es obligatoria") @Min(value = 1, message = "La cantidad debe ser al menos 1")
        Integer cantidad,
        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
        BigDecimal precioUnitario,
        @NotNull(message = "La tasa de IVA es obligatoria") TasaIva tasaIva) {
}
