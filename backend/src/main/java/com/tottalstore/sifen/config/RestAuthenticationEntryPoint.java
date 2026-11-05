package com.tottalstore.sifen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tottalstore.sifen.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * BUG-010: una request sin token válido (vencido, mal formado, o de un usuario desactivado) responde
 * 401 con el formato de {@link ApiError}. Sin este entry point Spring respondía 403, que el frontend no
 * podía distinguir de "autenticado pero sin permiso", y la sesión vencida nunca volvía al login.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(401, "UNAUTHORIZED", "Tu sesión venció o no es válida. Volvé a iniciar sesión.");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
