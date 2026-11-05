package com.tottalstore.sifen.establecimientos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Largos máximos de la tabla {@code establecimiento}. Departamento, distrito y ciudad llevan el
 * <b>código numérico</b> de la tabla de referencia del SIFEN, no el nombre; el nombre va en la
 * descripción. {@code activo} solo se usa al editar.
 */
public record EstablecimientoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "\\d{3}", message = "El código debe tener 3 dígitos (ej. 001)")
        String codigo,
        @NotBlank(message = "La denominación es obligatoria")
        @Size(max = 100, message = "La denominación admite hasta 100 caracteres")
        String denominacion,
        @Size(max = 255, message = "La dirección admite hasta 255 caracteres") String direccion,
        @Size(max = 10, message = "El número de casa admite hasta 10 caracteres") String numeroCasa,
        @Pattern(regexp = "\\d{0,5}", message = "El código de departamento es numérico, de hasta 5 dígitos (el nombre va en la descripción)")
        String departamentoCodigo,
        @Size(max = 50, message = "La descripción del departamento admite hasta 50 caracteres")
        String departamentoDescripcion,
        @Pattern(regexp = "\\d{0,5}", message = "El código de distrito es numérico, de hasta 5 dígitos (el nombre va en la descripción)")
        String distritoCodigo,
        @Size(max = 50, message = "La descripción del distrito admite hasta 50 caracteres")
        String distritoDescripcion,
        @Pattern(regexp = "\\d{0,5}", message = "El código de ciudad es numérico, de hasta 5 dígitos (el nombre va en la descripción)")
        String ciudadCodigo,
        @Size(max = 50, message = "La descripción de la ciudad admite hasta 50 caracteres")
        String ciudadDescripcion,
        @Size(max = 20, message = "El teléfono admite hasta 20 caracteres") String telefono,
        @Email(message = "El email del establecimiento no es válido")
        @Size(max = 100, message = "El email admite hasta 100 caracteres")
        String email,
        Boolean activo) {
}
