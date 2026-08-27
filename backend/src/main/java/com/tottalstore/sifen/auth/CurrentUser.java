package com.tottalstore.sifen.auth;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resuelve el {@link Usuario} autenticado en la petición actual, a partir del JWT ya validado. */
@Component
public class CurrentUser {

    private final UsuarioRepository usuarioRepository;

    public CurrentUser(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtener() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no encontrado: " + email));
    }
}
