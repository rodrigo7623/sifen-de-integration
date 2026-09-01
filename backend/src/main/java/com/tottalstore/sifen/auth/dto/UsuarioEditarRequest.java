package com.tottalstore.sifen.auth.dto;

import com.tottalstore.sifen.auth.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El email no se puede editar (es el identificador de login). La contraseña es opcional: si viene
 * nula o en blanco, se mantiene la actual sin cambios.
 */
public record UsuarioEditarRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres") String password) {
}
