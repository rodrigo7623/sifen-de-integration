package com.tottalstore.sifen.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** BUG-005: un token vigente no alcanza si el usuario ya no está activo. */
class JwtAuthFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthFilter filtro = new JwtAuthFilter(jwtService, userDetailsService);
    private final FilterChain chain = mock(FilterChain.class);
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-vigente");
        when(jwtService.esValido("token-vigente")).thenReturn(true);
        when(jwtService.extraerEmail("token-vigente")).thenReturn("cajero@tottalstore.com");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaAUnUsuarioActivo() throws Exception {
        when(userDetailsService.loadUserByUsername("cajero@tottalstore.com")).thenReturn(usuario(true));

        filtro.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void noAutenticaAUnUsuarioDesactivadoAunqueSuTokenSigaVigente() throws Exception {
        when(userDetailsService.loadUserByUsername("cajero@tottalstore.com")).thenReturn(usuario(false));

        filtro.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void unUsuarioBorradoNoTerminaEnErrorSinoSinAutenticar() throws Exception {
        when(userDetailsService.loadUserByUsername("cajero@tottalstore.com"))
                .thenThrow(new UsernameNotFoundException("Usuario no encontrado"));

        filtro.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(org.mockito.ArgumentMatchers.eq(request), org.mockito.ArgumentMatchers.any());
    }

    private org.springframework.security.core.userdetails.UserDetails usuario(boolean activo) {
        return User.withUsername("cajero@tottalstore.com")
                .password("x")
                .authorities("ROLE_CAJERO")
                .disabled(!activo)
                .build();
    }
}
