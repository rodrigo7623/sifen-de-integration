package com.tottalstore.sifen.clientes.dto;

import com.tottalstore.sifen.shared.CondicionIva;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteRequest(
        @NotBlank(message = "El RUC/CI es obligatorio") String ruc,
        @NotBlank(message = "La razón social es obligatoria") String razonSocial,
        String direccion,
        String email,
        @NotNull(message = "La condición ante el IVA es obligatoria") CondicionIva condicionIva) {
}
