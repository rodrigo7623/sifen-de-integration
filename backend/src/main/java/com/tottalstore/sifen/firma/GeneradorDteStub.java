package com.tottalstore.sifen.firma;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.sifen.Ambiente;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación stub (default): una representación mínima del DE firmada con
 * {@link FirmaDigitalStub}, solo para ejercitar el flujo con {@code EnviadorSifenStub}.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "stub", matchIfMissing = true)
public class GeneradorDteStub implements GeneradorDteService {

    private final FirmaDigitalService firmaDigitalService;

    public GeneradorDteStub(FirmaDigitalService firmaDigitalService) {
        this.firmaDigitalService = firmaDigitalService;
    }

    @Override
    public DteFirmado generar(FacturaElectronica factura, Ambiente ambiente) {
        return firmaDigitalService.firmar(construirXmlSimplificado(factura));
    }

    /** Incluir la descripción de los ítems permite forzar un rechazo simulado agregando la palabra
     * "RECHAZAR" en algún ítem (ver EnviadorSifenStub). */
    private String construirXmlSimplificado(FacturaElectronica factura) {
        StringBuilder items = new StringBuilder();
        for (ItemFactura item : factura.getItems()) {
            items.append("<item>").append(escapar(item.getDescripcion())).append("</item>");
        }
        String numero = factura.numeroComprobante();
        String asociada = factura.getFacturaAsociada() != null
                ? "<facturaAsociada>" + factura.getFacturaAsociada().getId() + "</facturaAsociada>" : "";
        return "<DTE><idFactura>" + factura.getId() + "</idFactura>"
                + "<tipo>" + factura.getTipoDoc() + "</tipo>" + asociada
                + "<numero>" + (numero != null ? numero : "") + "</numero>"
                + "<fechaEmision>" + factura.getFechaEmision() + "</fechaEmision>"
                + "<clienteRuc>" + escapar(factura.getCliente().getRuc()) + "</clienteRuc>"
                + "<totalGeneral>" + factura.getTotalGeneral() + "</totalGeneral>"
                + "<items>" + items + "</items></DTE>";
    }

    private static String escapar(String texto) {
        if (texto == null) return "";
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
