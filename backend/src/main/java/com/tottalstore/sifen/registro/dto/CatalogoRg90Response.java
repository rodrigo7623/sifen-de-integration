package com.tottalstore.sifen.registro.dto;

import com.tottalstore.sifen.registro.TipoComprobanteRg90;
import com.tottalstore.sifen.registro.TipoIdentificacion;
import com.tottalstore.sifen.registro.TipoRegistro;
import java.util.Arrays;
import java.util.List;

/** Tablas 3 y 4 de la RG 90 para los combos del formulario, con las reglas que dependen del tipo. */
public record CatalogoRg90Response(List<TipoComprobante> tiposComprobante, List<Identificacion> tiposIdentificacion) {

    public record TipoComprobante(
            String valor, int codigo, String descripcion, String sigla, boolean ventas, boolean compras,
            boolean soloTotalEnCompras, boolean numeroOpcional, boolean sinTimbrado, boolean esNota,
            boolean exigeRucEnCompras) {
    }

    public record Identificacion(String valor, int codigo, String descripcion, boolean nombreOpcional) {
    }

    public static CatalogoRg90Response crear() {
        List<TipoComprobante> tipos = Arrays.stream(TipoComprobanteRg90.values())
                .map(t -> new TipoComprobante(t.name(), t.codigo(), t.descripcion(), t.sigla(),
                        t.admite(TipoRegistro.VENTA), t.admite(TipoRegistro.COMPRA), t.soloTotal(TipoRegistro.COMPRA),
                        t.numeroOpcional(), t.sinTimbrado(), t.esNota(), t.exigeRucEnCompras()))
                .toList();
        List<Identificacion> ids = Arrays.stream(TipoIdentificacion.values())
                .map(t -> new Identificacion(t.name(), t.codigo(), t.descripcion(), t.nombreOpcional()))
                .toList();
        return new CatalogoRg90Response(tipos, ids);
    }
}
