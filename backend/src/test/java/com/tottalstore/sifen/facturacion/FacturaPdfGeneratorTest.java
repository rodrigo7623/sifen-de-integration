package com.tottalstore.sifen.facturacion;

import static org.assertj.core.api.Assertions.assertThat;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.shared.CondicionIva;
import com.tottalstore.sifen.shared.TasaIva;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class FacturaPdfGeneratorTest {

    private final FacturaPdfGenerator generator = new FacturaPdfGenerator();

    @Test
    void generaUnPdfConLosDatosDeLaFactura() throws IOException {
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(UUID.randomUUID());
        factura.setEstadoDte(EstadoDte.APROBADO);
        factura.setTotalIva10(new BigDecimal("113636"));
        factura.setTotalGeneral(new BigDecimal("1250000"));

        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-0");
        cliente.setRazonSocial("Distribuidora Sur SA");
        cliente.setCondicionIva(CondicionIva.RESPONSABLE_IVA);
        factura.setCliente(cliente);

        ItemFactura item = new ItemFactura();
        item.setDescripcion("Taladro percutor 220V");
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("350000"));
        item.setTasaIva(TasaIva.DIEZ);
        item.setSubtotal(new BigDecimal("700000"));
        factura.getItems().add(item);

        byte[] pdf = generator.generar(factura);

        assertThat(pdf).isNotEmpty();
        String texto = extraerTexto(pdf);
        assertThat(texto).contains("80012345-0");
        assertThat(texto).contains("Distribuidora Sur SA");
        assertThat(texto).contains("Taladro percutor 220V");
        assertThat(texto).contains("APROBADO");
    }

    private String extraerTexto(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
