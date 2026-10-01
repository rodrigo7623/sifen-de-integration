package com.tottalstore.sifen.sifen;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Datos del emisor (Tottal Store) necesarios para armar el CDC y, más adelante (Fase 3), el XML
 * completo del DE. Solo lo estrictamente necesario para el CDC por ahora: RUC, tipo de
 * contribuyente, establecimiento y punto de expedición — el número de timbrado y sus fechas de
 * vigencia no forman parte del CDC (ver sección 10.1 del manual), se agregan recién cuando se
 * construya el grupo C (Timbrado) del XML real.
 *
 * <p>Sin valores por defecto para el RUC/tipo de contribuyente a propósito (son datos reales del
 * contribuyente). Establecimiento/punto de expedición sí tienen un default provisorio ("001") -- el
 * SIFEN exige haber probado en el ambiente de test antes de poder solicitar el timbrado electrónico
 * definitivo, así que estos valores son placeholders hasta contar con los datos reales.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class EmisorConfig {

    private final String rucBase;
    private final int dvRuc;
    private final int tipoContribuyente;
    private final String establecimiento;
    private final String puntoExpedicion;

    public EmisorConfig(
            @Value("${sifen.emisor.ruc}") String ruc,
            @Value("${sifen.emisor.tipo-contribuyente}") int tipoContribuyente,
            @Value("${sifen.emisor.establecimiento:001}") String establecimiento,
            @Value("${sifen.emisor.punto-expedicion:001}") String puntoExpedicion) {
        if (ruc == null || ruc.isBlank()) {
            throw new IllegalStateException(
                    "sifen.envio.modo=real requiere SIFEN_EMISOR_RUC (RUC del emisor, formato base-DV)");
        }
        String[] partes = ruc.trim().split("-");
        if (partes.length != 2) {
            throw new IllegalStateException(
                    "SIFEN_EMISOR_RUC debe tener el formato base-DV (ej. 4309652-2), recibido: " + ruc);
        }
        this.rucBase = partes[0];
        this.dvRuc = Integer.parseInt(partes[1]);
        this.tipoContribuyente = tipoContribuyente;
        this.establecimiento = establecimiento;
        this.puntoExpedicion = puntoExpedicion;
    }

    public String rucBase() {
        return rucBase;
    }

    public int dvRuc() {
        return dvRuc;
    }

    public int tipoContribuyente() {
        return tipoContribuyente;
    }

    public String establecimiento() {
        return establecimiento;
    }

    public String puntoExpedicion() {
        return puntoExpedicion;
    }
}
