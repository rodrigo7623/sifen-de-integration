package com.tottalstore.sifen.configuracion.dto;

import java.time.LocalDate;

/** Sin validaciones obligatorias: el admin puede ir completando estos datos de forma incremental
 * (p.ej. todavía sin el número de timbrado, hasta terminar las pruebas de homologación). */
public record ConfiguracionEmisorRequest(
        String razonSocial,
        String nombreFantasia,
        String direccion,
        String numeroCasa,
        String complementoDireccion1,
        String complementoDireccion2,
        String departamentoCodigo,
        String departamentoDescripcion,
        String distritoCodigo,
        String distritoDescripcion,
        String ciudadCodigo,
        String ciudadDescripcion,
        String telefono,
        String email,
        String actividadEconomicaCodigo,
        String actividadEconomicaDescripcion,
        String rucBase,
        Integer dvRuc,
        Integer tipoContribuyente,
        String timbradoNumero,
        LocalDate timbradoFechaInicio,
        LocalDate timbradoFechaFin) {
}
