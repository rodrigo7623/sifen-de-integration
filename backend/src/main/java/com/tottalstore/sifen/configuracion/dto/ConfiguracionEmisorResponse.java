package com.tottalstore.sifen.configuracion.dto;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import java.time.LocalDate;

public record ConfiguracionEmisorResponse(
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

    public static ConfiguracionEmisorResponse from(ConfiguracionEmisor c) {
        return new ConfiguracionEmisorResponse(
                c.getRazonSocial(),
                c.getNombreFantasia(),
                c.getDireccion(),
                c.getNumeroCasa(),
                c.getComplementoDireccion1(),
                c.getComplementoDireccion2(),
                c.getDepartamentoCodigo(),
                c.getDepartamentoDescripcion(),
                c.getDistritoCodigo(),
                c.getDistritoDescripcion(),
                c.getCiudadCodigo(),
                c.getCiudadDescripcion(),
                c.getTelefono(),
                c.getEmail(),
                c.getActividadEconomicaCodigo(),
                c.getActividadEconomicaDescripcion(),
                c.getRucBase(),
                c.getDvRuc(),
                c.getTipoContribuyente(),
                c.getTimbradoNumero(),
                c.getTimbradoFechaInicio(),
                c.getTimbradoFechaFin());
    }
}
