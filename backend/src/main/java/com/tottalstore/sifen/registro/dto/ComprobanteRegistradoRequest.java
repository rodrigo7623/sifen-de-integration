package com.tottalstore.sifen.registro.dto;

import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.registro.TipoComprobanteRg90;
import com.tottalstore.sifen.registro.TipoIdentificacion;
import com.tottalstore.sifen.registro.TipoRegistro;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta o edición de un comprobante del Registro de Comprobantes (RG 90/2021). Los largos son los de la
 * Especificación Técnica de importación; las reglas que dependen del tipo de comprobante las controla
 * {@code ComprobanteRegistradoService}.
 */
public record ComprobanteRegistradoRequest(
        @NotNull(message = "Indicá si es una compra o una venta") TipoRegistro tipoRegistro,
        @NotNull(message = "El tipo de comprobante es obligatorio") TipoComprobanteRg90 tipoComprobante,
        @NotNull(message = "La fecha de emisión es obligatoria") LocalDate fechaEmision,
        @NotNull(message = "El tipo de identificación es obligatorio") TipoIdentificacion tipoIdentificacion,
        @NotBlank(message = "El número de identificación es obligatorio")
        @Size(max = 20, message = "La identificación admite hasta 20 caracteres")
        String identificacion,
        @Size(max = 250, message = "La razón social admite hasta 250 caracteres") String razonSocial,
        @Pattern(regexp = "\\d{0,8}", message = "El timbrado tiene hasta 8 dígitos") String timbrado,
        @Pattern(regexp = "(\\d{3}-\\d{3}-\\d{7})?", message = "El número de comprobante va con el formato 001-001-0000001")
        String numero,
        @Pattern(regexp = "(\\d{44})?", message = "El CDC tiene 44 dígitos") String cdc,
        CondicionPago condicion,
        boolean monedaExtranjera,
        @PositiveOrZero(message = "El gravado 10% no puede ser negativo")
        @Digits(integer = 18, fraction = 0, message = "Los montos van en guaraníes, sin decimales")
        BigDecimal gravado10,
        @PositiveOrZero(message = "El gravado 5% no puede ser negativo")
        @Digits(integer = 18, fraction = 0, message = "Los montos van en guaraníes, sin decimales")
        BigDecimal gravado5,
        @PositiveOrZero(message = "El exento no puede ser negativo")
        @Digits(integer = 18, fraction = 0, message = "Los montos van en guaraníes, sin decimales")
        BigDecimal exento,
        @NotNull(message = "El total es obligatorio")
        @Digits(integer = 18, fraction = 0, message = "Los montos van en guaraníes, sin decimales")
        BigDecimal total,
        boolean imputaIva,
        boolean imputaIre,
        boolean imputaIrp,
        boolean noImputa,
        @Pattern(regexp = "(\\d{3}-\\d{3}-\\d{7})?", message = "El comprobante asociado va con el formato 001-001-0000001")
        String asociadoNumero,
        @Pattern(regexp = "\\d{0,8}", message = "El timbrado del comprobante asociado tiene hasta 8 dígitos")
        String asociadoTimbrado,
        @Size(max = 250, message = "La observación admite hasta 250 caracteres") String observacion) {
}
