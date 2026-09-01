package com.tottalstore.sifen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tottalstore.sifen.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Traduce un 403 de Spring Security (usuario autenticado pero sin el rol requerido) al mismo
 * formato de {@link ApiError} que usa el resto de la API, en vez de la respuesta vacía por defecto.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(403, "ACCESS_DENIED", "No tenés permisos para realizar esta acción");
        objectMapper.writeValue(response.getWriter(), error);
    }
}
