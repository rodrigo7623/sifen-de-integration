package com.tottalstore.sifen.clientes.dto;

import com.tottalstore.sifen.shared.CondicionIva;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Largos máximos de la tabla {@code cliente}. {@code activo} solo se usa al editar (reactivar). */
public record ClienteRequest(
        @NotBlank(message = "El RUC/CI es obligatorio")
        @Size(max = 20, message = "El RUC/CI admite hasta 20 caracteres")
        String ruc,
        @NotBlank(message = "La razón social es obligatoria")
        @Size(max = 200, message = "La razón social admite hasta 200 caracteres")
        String razonSocial,
        @Size(max = 250, message = "La dirección admite hasta 250 caracteres") String direccion,
        @Email(message = "El email del cliente no es válido")
        @Size(max = 150, message = "El email admite hasta 150 caracteres")
        String email,
        @NotNull(message = "La condición ante el IVA es obligatoria") CondicionIva condicionIva,
        Boolean activo) {
}
