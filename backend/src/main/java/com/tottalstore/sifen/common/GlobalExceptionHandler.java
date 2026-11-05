package com.tottalstore.sifen.common;

import java.util.regex.Matcher;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "BUSINESS_RULE_VIOLATION", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "INVALID_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "INVALID_CREDENTIALS", "Usuario o contraseña incorrectos"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex) {
        // Lanzada por Spring Security en el chequeo previo al login cuando Usuario.activo = false
        // (ver UsuarioDetailsServiceImpl.disabled(...)). Sin este handler cae en el genérico de 500.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "ACCOUNT_DISABLED", "La cuenta está desactivada"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                // Los mensajes ya nombran el campo ("La denominación admite hasta 100 caracteres"), así
                // que se muestran tal cual en pantalla, sin el nombre técnico del atributo.
                .map(err -> err.getDefaultMessage())
                .distinct()
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "VALIDATION_ERROR", "Datos inválidos", detalles));
    }

    // BUG-012: un body o parámetro con formato inválido (JSON roto, enum desconocido, periodo=octubre,
    // un UUID mal escrito) o faltante es un error del pedido, no del servidor.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "INVALID_REQUEST", "El cuerpo del pedido no es un JSON válido para esta operación"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "INVALID_REQUEST",
                        "Valor inválido para '" + ex.getName() + "': " + ex.getValue()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "INVALID_REQUEST", "Falta el parámetro '" + ex.getParameterName() + "'"));
    }

    private static final java.util.regex.Pattern LARGO_MAXIMO =
            java.util.regex.Pattern.compile("(?:character varying|varchar)\\((\\d+)\\)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Red de seguridad para lo que la validación de los DTO no haya atajado (antes terminaba en el 500
     * "Ocurrió un error inesperado"): dato más largo que la columna, valor duplicado o referencia rota.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        String causa = String.valueOf(ex.getMostSpecificCause().getMessage());
        log.warn("Violación de integridad de datos: {}", causa);
        String texto = causa.toLowerCase();
        Matcher largo = LARGO_MAXIMO.matcher(causa);
        if (texto.contains("demasiado largo") || texto.contains("too long")) {
            String maximo = largo.find() ? " (máximo " + largo.group(1) + " caracteres)" : "";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(400, "VALIDATION_ERROR",
                    "Uno de los datos ingresados supera el largo permitido" + maximo + ". Revisá los campos del formulario."));
        }
        if (texto.contains("llave duplicada") || texto.contains("duplicate key") || texto.contains("unique")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, "DUPLICADO",
                    "Ya existe un registro con esos datos (código, RUC o email repetido)."));
        }
        if (texto.contains("llave foránea") || texto.contains("foreign key")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, "REFERENCIA",
                    "La operación hace referencia a un registro que no existe o que está en uso."));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(400, "VALIDATION_ERROR",
                "Los datos no cumplen las restricciones de la base de datos. Revisá los campos del formulario."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "INTERNAL_ERROR", "Ocurrió un error inesperado"));
    }
}
