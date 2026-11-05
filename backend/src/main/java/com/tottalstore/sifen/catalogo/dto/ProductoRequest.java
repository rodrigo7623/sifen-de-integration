package com.tottalstore.sifen.catalogo.dto;

import com.tottalstore.sifen.shared.TasaIva;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Los largos máximos son los de la tabla {@code producto}; la descripción se limita a 120 porque pasa
 * tal cual al ítem del DE (E708 {@code dDesProSer}, 1-120 caracteres).
 * {@code activo} solo se tiene en cuenta al editar (permite reactivar un producto dado de baja).
 */
public record ProductoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Size(max = 50, message = "El código admite hasta 50 caracteres")
        String codigo,
        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 120, message = "La descripción admite hasta 120 caracteres (límite del SIFEN)")
        String descripcion,
        @NotBlank(message = "La unidad de medida es obligatoria")
        @Size(max = 20, message = "La unidad de medida admite hasta 20 caracteres")
        String unidadMedida,
        @NotNull(message = "El precio base es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a cero")
        @Digits(integer = 13, fraction = 2, message = "El precio admite hasta 13 dígitos enteros")
        BigDecimal precioBase,
        @NotNull(message = "La tasa de IVA es obligatoria") TasaIva tasaIva,
        Boolean activo) {
}
