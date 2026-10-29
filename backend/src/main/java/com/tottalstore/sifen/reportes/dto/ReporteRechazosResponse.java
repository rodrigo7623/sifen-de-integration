package com.tottalstore.sifen.reportes.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Estadísticas de rechazo del SIFEN en un rango de fechas (Release 3), sobre cada intento de envío. */
public record ReporteRechazosResponse(
        LocalDate desde,
        LocalDate hasta,
        long totalIntentos,
        long aprobados,
        long rechazados,
        /** Porcentaje de intentos aprobados, 0–100; null si no hubo intentos. */
        Double tasaAprobacion,
        long facturasConRechazo,
        List<PorCodigo> porCodigo,
        List<PorMes> porMes,
        List<Rechazo> ultimosRechazos) {

    public record PorCodigo(String codigo, String mensaje, long cantidad, Instant ultimaVez) {
    }

    public record PorMes(String mes, long intentos, long aprobados, long rechazados) {
    }

    public record Rechazo(
            UUID facturaId, Instant fecha, String clienteRuc, String clienteRazonSocial, String codigo, String mensaje) {
    }
}
