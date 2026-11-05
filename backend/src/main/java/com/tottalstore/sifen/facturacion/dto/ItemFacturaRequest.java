package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.shared.TasaIva;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * {@code productoCodigo} es opcional: si se informa, el ítem proviene del catálogo (RF-12); si no,
 * es un ítem cargado ad-hoc (RF-13) y {@code descripcion}/{@code precioUnitario}/{@code tasaIva}
 * son obligatorios. La descripción va al DE como E708 {@code dDesProSer} (1-120 caracteres).
 */
public record ItemFacturaRequest(
        @Size(max = 50, message = "El código de producto admite hasta 50 caracteres") String productoCodigo,
        @NotBlank(message = "La descripción del ítem es obligatoria")
        @Size(max = 120, message = "La descripción del ítem admite hasta 120 caracteres (límite del SIFEN)")
        String descripcion,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        @Max(value = 999_999, message = "La cantidad admite hasta 999.999")
        Integer cantidad,
        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El precio unitario admite hasta 13 dígitos enteros")
        BigDecimal precioUnitario,
        @NotNull(message = "La tasa de IVA es obligatoria") TasaIva tasaIva) {
}
