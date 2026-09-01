package com.tottalstore.sifen.auth.dto;

import com.tottalstore.sifen.auth.Rol;
import com.tottalstore.sifen.auth.Usuario;
import java.util.UUID;

public record UsuarioResponse(UUID id, String nombre, String email, Rol rol, boolean activo) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getRol(), u.isActivo());
    }
}
