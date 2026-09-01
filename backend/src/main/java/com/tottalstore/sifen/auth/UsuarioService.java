package com.tottalstore.sifen.auth;

import com.tottalstore.sifen.auth.dto.UsuarioCrearRequest;
import com.tottalstore.sifen.auth.dto.UsuarioEditarRequest;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Alta, baja y modificación de usuarios del panel — accesible solo para ADMIN (RNF-04). */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public UsuarioService(
            UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, CurrentUser currentUser) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    public List<Usuario> buscar(String q) {
        if (q == null || q.isBlank()) {
            return usuarioRepository.findAllByOrderByNombreAsc();
        }
        return usuarioRepository.buscar(q.trim());
    }

    public Usuario obtener(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    public Usuario crear(UsuarioCrearRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("Ya existe un usuario con el email " + request.email());
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(request.rol());
        usuario.setActivo(true);
        return usuarioRepository.save(usuario);
    }

    public Usuario editar(UUID id, UsuarioEditarRequest request) {
        Usuario usuario = obtener(id);
        usuario.setNombre(request.nombre());
        usuario.setRol(request.rol());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return usuarioRepository.save(usuario);
    }

    public void activar(UUID id) {
        Usuario usuario = obtener(id);
        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    public void desactivar(UUID id) {
        Usuario usuario = obtener(id);
        if (usuario.getId().equals(currentUser.obtener().getId())) {
            throw new BusinessException("No podés desactivar tu propio usuario");
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }
}
