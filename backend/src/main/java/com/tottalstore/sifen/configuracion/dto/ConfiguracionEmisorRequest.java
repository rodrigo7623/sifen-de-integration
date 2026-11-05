package com.tottalstore.sifen.configuracion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Sin campos obligatorios: el admin puede ir completando estos datos de forma incremental (lo que
 * falte para emitir lo avisa el generador del DE). Sí se validan formato y largo de cada columna de
 * {@code configuracion_emisor}, para devolver un mensaje claro en vez de un error de la base.
 *
 * <p>{@code actividadesEconomicas} reemplaza al par {@code actividadEconomicaCodigo/Descripcion}
 * (que se mantiene por compatibilidad: si la lista viene nula, ese par pasa a ser la única actividad).
 */
public record ConfiguracionEmisorRequest(
        @Size(max = 255, message = "La razón social admite hasta 255 caracteres") String razonSocial,
        @Size(max = 255, message = "El nombre de fantasía admite hasta 255 caracteres") String nombreFantasia,
        @Size(max = 255, message = "La dirección admite hasta 255 caracteres") String direccion,
        @Size(max = 10, message = "El número de casa admite hasta 10 caracteres") String numeroCasa,
        @Size(max = 255, message = "El complemento de dirección 1 admite hasta 255 caracteres") String complementoDireccion1,
        @Size(max = 255, message = "El complemento de dirección 2 admite hasta 255 caracteres") String complementoDireccion2,
        @Pattern(regexp = "\\d{0,5}", message = "El código de departamento es numérico, de hasta 5 dígitos (el nombre va en la descripción)")
        String departamentoCodigo,
        @Size(max = 50, message = "La descripción del departamento admite hasta 50 caracteres") String departamentoDescripcion,
        @Pattern(regexp = "\\d{0,5}", message = "El código de distrito es numérico, de hasta 5 dígitos (el nombre va en la descripción)")
        String distritoCodigo,
        @Size(max = 50, message = "La descripción del distrito admite hasta 50 caracteres") String distritoDescripcion,
        @Pattern(regexp = "\\d{0,5}", message = "El código de ciudad es numérico, de hasta 5 dígitos (el nombre va en la descripción)")
        String ciudadCodigo,
        @Size(max = 50, message = "La descripción de la ciudad admite hasta 50 caracteres") String ciudadDescripcion,
        @Size(max = 20, message = "El teléfono admite hasta 20 caracteres") String telefono,
        @Email(message = "El email del emisor no es válido")
        @Size(max = 100, message = "El email admite hasta 100 caracteres")
        String email,
        @Size(max = 10, message = "El código de la actividad económica admite hasta 10 caracteres") String actividadEconomicaCodigo,
        @Size(max = 300, message = "La descripción de la actividad económica admite hasta 300 caracteres") String actividadEconomicaDescripcion,
        @Pattern(regexp = "\\d{0,8}", message = "El RUC (base) es numérico, de hasta 8 dígitos, sin el dígito verificador")
        String rucBase,
        @Min(value = 0, message = "El dígito verificador va de 0 a 9")
        @Max(value = 9, message = "El dígito verificador va de 0 a 9")
        Integer dvRuc,
        @Min(value = 1, message = "El tipo de contribuyente es 1 (persona física) o 2 (persona jurídica)")
        @Max(value = 2, message = "El tipo de contribuyente es 1 (persona física) o 2 (persona jurídica)")
        Integer tipoContribuyente,
        @Pattern(regexp = "\\d{0,8}", message = "El número de timbrado es numérico, de hasta 8 dígitos")
        String timbradoNumero,
        LocalDate timbradoFechaInicio,
        LocalDate timbradoFechaFin,
        @Valid
        @Size(max = 9, message = "El SIFEN admite hasta 9 actividades económicas")
        List<ActividadEconomicaRequest> actividadesEconomicas) {
}
