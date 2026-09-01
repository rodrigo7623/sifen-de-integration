package com.tottalstore.sifen.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auth.dto.UsuarioCrearRequest;
import com.tottalstore.sifen.auth.dto.UsuarioEditarRequest;
import com.tottalstore.sifen.common.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CurrentUser currentUser;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, currentUser);
    }

    @Test
    void creaUsuarioConPasswordHasheado() {
        UsuarioCrearRequest request = new UsuarioCrearRequest("Ana Operaria", "ana@tottalstore.com", "clave123", Rol.OPERARIO);
        when(usuarioRepository.existsByEmail("ana@tottalstore.com")).thenReturn(false);
        when(passwordEncoder.encode("clave123")).thenReturn("hash-simulado");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario creado = usuarioService.crear(request);

        assertThat(creado.getEmail()).isEqualTo("ana@tottalstore.com");
        assertThat(creado.getPasswordHash()).isEqualTo("hash-simulado");
        assertThat(creado.isActivo()).isTrue();
    }

    @Test
    void rechazaEmailDuplicado() {
        UsuarioCrearRequest request = new UsuarioCrearRequest("Ana", "ana@tottalstore.com", "clave123", Rol.OPERARIO);
        when(usuarioRepository.existsByEmail("ana@tottalstore.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ana@tottalstore.com");
    }

    @Test
    void editarSinPasswordNoCambiaElHashActual() {
        UUID id = UUID.randomUUID();
        Usuario existente = new Usuario();
        existente.setId(id);
        existente.setPasswordHash("hash-original");
        when(usuarioRepository.findById(id)).thenReturn(java.util.Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioEditarRequest request = new UsuarioEditarRequest("Ana Editada", Rol.ADMIN, null);
        Usuario editado = usuarioService.editar(id, request);

        assertThat(editado.getNombre()).isEqualTo("Ana Editada");
        assertThat(editado.getPasswordHash()).isEqualTo("hash-original");
    }

    @Test
    void noPermiteDesactivarseASiMismo() {
        UUID id = UUID.randomUUID();
        Usuario propioUsuario = new Usuario();
        propioUsuario.setId(id);
        when(usuarioRepository.findById(id)).thenReturn(java.util.Optional.of(propioUsuario));
        when(currentUser.obtener()).thenReturn(propioUsuario);

        assertThatThrownBy(() -> usuarioService.desactivar(id))
                .isInstanceOf(BusinessException.class);
    }
}
