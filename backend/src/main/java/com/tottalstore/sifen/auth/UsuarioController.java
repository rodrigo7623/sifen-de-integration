package com.tottalstore.sifen.auth;

import com.tottalstore.sifen.auth.dto.UsuarioCrearRequest;
import com.tottalstore.sifen.auth.dto.UsuarioEditarRequest;
import com.tottalstore.sifen.auth.dto.UsuarioResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ABM de usuarios del panel. Todas las rutas están restringidas a ADMIN en {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> buscar(@RequestParam(required = false) String q) {
        return usuarioService.buscar(q).stream().map(UsuarioResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioCrearRequest request) {
        return UsuarioResponse.from(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public UsuarioResponse editar(@PathVariable UUID id, @Valid @RequestBody UsuarioEditarRequest request) {
        return UsuarioResponse.from(usuarioService.editar(id, request));
    }

    @PostMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@PathVariable UUID id) {
        usuarioService.activar(id);
    }

    @PostMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable UUID id) {
        usuarioService.desactivar(id);
    }
}
