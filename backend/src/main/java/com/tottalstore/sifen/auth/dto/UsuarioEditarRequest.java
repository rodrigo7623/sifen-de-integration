package com.tottalstore.sifen.auth.dto;

import com.tottalstore.sifen.auth.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El email no se puede editar (es el identificador de login). La contraseña es opcional: si viene
 * nula o en blanco, se mantiene la actual sin cambios. {@code activo} permite reactivar o desactivar
 * desde la misma edición (nulo = sin cambios).
 */
public record UsuarioEditarRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre admite hasta 150 caracteres")
        String nombre,
        @NotNull(message = "El rol es obligatorio") Rol rol,
        @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres") String password,
        Boolean activo) {
}
