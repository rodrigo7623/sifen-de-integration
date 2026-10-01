package com.tottalstore.sifen.establecimientos.dto;

import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import java.util.UUID;

public record PuntoExpedicionResponse(UUID id, String codigo, String descripcion, boolean activo) {

    public static PuntoExpedicionResponse from(PuntoExpedicion p) {
        return new PuntoExpedicionResponse(p.getId(), p.getCodigo(), p.getDescripcion(), p.isActivo());
    }
}
