package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.shared.TasaIva;
import java.math.BigDecimal;

public record ItemFacturaResponse(
        String productoCodigo,
        String descripcion,
        Integer cantidad,
        BigDecimal precioUnitario,
        TasaIva tasaIva,
        BigDecimal subtotal) {

    public static ItemFacturaResponse from(ItemFactura item) {
        return new ItemFacturaResponse(
                item.getProducto() != null ? item.getProducto().getCodigo() : null,
                item.getDescripcion(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                item.getTasaIva(),
                item.getSubtotal());
    }
}
