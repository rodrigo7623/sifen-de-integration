package com.tottalstore.sifen.registro;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tabla 4 de la Especificación Técnica del Registro de Comprobantes (RG 90/2021), limitada a los
 * comprobantes de ventas y compras (los 2xx son de ingresos/egresos del IRP, que no aplican a una
 * empresa con IRE e IVA como Tottal Store).
 */
public enum TipoComprobanteRg90 {
    AUTOFACTURA(101, "Autofactura", "AF", false, true),
    BOLETA_TRANSPORTE_PUBLICO(102, "Boleta de transporte público de pasajeros", "BTP", true, true),
    BOLETA_VENTA(103, "Boleta de venta", "BV", true, true),
    BOLETA_RESIMPLE(104, "Boleta resimple", "BR", false, true),
    LOTERIA(105, "Boletos de loterías, juegos de azar", "LOT", true, true),
    TICKET_AEREO(106, "Boleto o ticket de transporte aéreo", "TA", true, true),
    DESPACHO_IMPORTACION(107, "Despacho de importación", "DI", false, true),
    ENTRADA_ESPECTACULOS(108, "Entrada a espectáculos públicos", "EEP", true, true),
    FACTURA(109, "Factura", "FAC", true, true),
    NOTA_CREDITO(110, "Nota de crédito", "NC", true, true),
    NOTA_DEBITO(111, "Nota de débito", "ND", true, true),
    TICKET(112, "Ticket máquina registradora", "TKT", true, true);

    /** En compras, estos solo informan el total (los montos por tasa van en 0) y no dan crédito fiscal. */
    private static final Set<TipoComprobanteRg90> SOLO_TOTAL_EN_COMPRAS =
            EnumSet.of(AUTOFACTURA, BOLETA_RESIMPLE, LOTERIA, TICKET);

    private final int codigo;
    private final String descripcion;
    private final String sigla;
    private final boolean ventas;
    private final boolean compras;

    TipoComprobanteRg90(int codigo, String descripcion, String sigla, boolean ventas, boolean compras) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.sigla = sigla;
        this.ventas = ventas;
        this.compras = compras;
    }

    public int codigo() {
        return codigo;
    }

    public String descripcion() {
        return descripcion;
    }

    public String sigla() {
        return sigla;
    }

    public boolean admite(TipoRegistro registro) {
        return registro == TipoRegistro.VENTA ? ventas : compras;
    }

    public boolean soloTotal(TipoRegistro registro) {
        return registro == TipoRegistro.COMPRA && SOLO_TOTAL_EN_COMPRAS.contains(this);
    }

    /** Ticket de máquina registradora, ticket aéreo y despacho de importación no llevan número ###-###-#######. */
    public boolean numeroOpcional() {
        return this == TICKET || this == TICKET_AEREO || this == DESPACHO_IMPORTACION;
    }

    /** El despacho de importación no tiene timbrado: se informa 0. */
    public boolean sinTimbrado() {
        return this == DESPACHO_IMPORTACION;
    }

    public boolean esNota() {
        return this == NOTA_CREDITO || this == NOTA_DEBITO;
    }

    /** En compras, el proveedor siempre es RUC salvo autofactura y despacho de importación. */
    public boolean exigeRucEnCompras() {
        return this != AUTOFACTURA && this != DESPACHO_IMPORTACION;
    }
}
