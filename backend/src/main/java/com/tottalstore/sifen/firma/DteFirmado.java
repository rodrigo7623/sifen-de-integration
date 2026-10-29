package com.tottalstore.sifen.firma;

import java.time.Instant;

/**
 * DE listo para enviar al SIFEN. {@code cdc} y {@code codigoSeguridad} solo vienen con la
 * implementación real ({@code GeneradorDteSifen}); el stub no genera CDC.
 */
public record DteFirmado(String xmlFirmado, Instant fechaFirma, String cdc, String codigoSeguridad) {

    public DteFirmado(String xmlFirmado, Instant fechaFirma) {
        this(xmlFirmado, fechaFirma, null, null);
    }
}
