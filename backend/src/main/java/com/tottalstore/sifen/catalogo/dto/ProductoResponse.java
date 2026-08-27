package com.tottalstore.sifen.catalogo.dto;

import com.tottalstore.sifen.catalogo.Producto;
import com.tottalstore.sifen.shared.TasaIva;
import java.math.BigDecimal;

public record ProductoResponse(
        String codigo,
        String descripcion,
        String unidadMedida,
        BigDecimal precioBase,
        TasaIva tasaIva,
        boolean activo) {

    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getCodigo(), p.getDescripcion(), p.getUnidadMedida(),
                p.getPrecioBase(), p.getTasaIva(), p.isActivo());
    }
}
