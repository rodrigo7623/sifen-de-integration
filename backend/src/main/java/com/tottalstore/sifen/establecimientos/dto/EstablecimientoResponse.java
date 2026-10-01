package com.tottalstore.sifen.establecimientos.dto;

import com.tottalstore.sifen.establecimientos.Establecimiento;
import java.util.List;
import java.util.UUID;

public record EstablecimientoResponse(
        UUID id,
        String codigo,
        String denominacion,
        String direccion,
        String numeroCasa,
        String departamentoCodigo,
        String departamentoDescripcion,
        String distritoCodigo,
        String distritoDescripcion,
        String ciudadCodigo,
        String ciudadDescripcion,
        String telefono,
        String email,
        boolean activo,
        List<PuntoExpedicionResponse> puntosExpedicion) {

    public static EstablecimientoResponse from(Establecimiento e) {
        return new EstablecimientoResponse(
                e.getId(),
                e.getCodigo(),
                e.getDenominacion(),
                e.getDireccion(),
                e.getNumeroCasa(),
                e.getDepartamentoCodigo(),
                e.getDepartamentoDescripcion(),
                e.getDistritoCodigo(),
                e.getDistritoDescripcion(),
                e.getCiudadCodigo(),
                e.getCiudadDescripcion(),
                e.getTelefono(),
                e.getEmail(),
                e.isActivo(),
                e.getPuntosExpedicion().stream().map(PuntoExpedicionResponse::from).toList());
    }
}
