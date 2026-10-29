package com.tottalstore.sifen.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.shared.CondicionIva;
import com.tottalstore.sifen.shared.TasaIva;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * KuDE armado sin XML firmado del SIFEN (vista previa / modo stub). El caso con XML firmado real se
 * prueba en {@code GeneradorDteSifenTest}, que es donde se genera ese XML.
 */
class FacturaPdfGeneratorTest {

    private FacturaPdfGenerator generator;

    @BeforeEach
    void setUp() {
        ConfiguracionEmisor emisor = new ConfiguracionEmisor();
        emisor.setRucBase("4309652");
        emisor.setDvRuc(2);
        emisor.setRazonSocial("BARTOLOME RAMIREZ MEDINA");
        emisor.setNombreFantasia("TOTTAL STORE");
        emisor.setTimbradoNumero("04309652");
        emisor.setTimbradoFechaInicio(LocalDate.of(2026, 10, 1));
        ConfiguracionEmisorService service = mock(ConfiguracionEmisorService.class);
        when(service.obtenerOCrear()).thenReturn(emisor);
        generator = new FacturaPdfGenerator(service);
    }

    @Test
    void muestraNumeroDeFacturaTimbradoEmisorClienteEItems() throws IOException {
        FacturaElectronica factura = factura(EstadoDte.APROBADO);
        RespuestaSifen respuesta = new RespuestaSifen();
        respuesta.setXmlFirmado("<DTE><idFactura>x</idFactura></DTE>"); // XML simplificado del modo stub
        factura.setRespuestaSifen(respuesta);

        String texto = extraerTexto(generator.generar(factura));

        assertThat(texto).contains("FACTURA ELECTRÓNICA");
        assertThat(texto).contains("N° 001-001-0000007");
        assertThat(texto).contains("Timbrado N° 04309652");
        assertThat(texto).contains("Fecha de inicio de vigencia: 01/10/2026");
        assertThat(texto).contains("RUC: 4309652-2");
        assertThat(texto).contains("BARTOLOME RAMIREZ MEDINA");
        assertThat(texto).contains("80012345-0");
        assertThat(texto).contains("Distribuidora Sur SA");
        assertThat(texto).contains("Taladro percutor 220V");
        assertThat(texto).contains("700.000");
        assertThat(texto).contains("TOTAL IVA: 63.636");
        assertThat(texto).contains("VISTA PREVIA");
    }

    @Test
    void unaFacturaRechazadaSeMarcaSinValidez() throws IOException {
        String texto = extraerTexto(generator.generar(factura(EstadoDte.RECHAZADO)));

        assertThat(texto).contains("DOCUMENTO NO APROBADO POR EL SIFEN (estado RECHAZADO)");
        assertThat(texto).contains("CDC: (sin CDC)");
    }

    @Test
    void muchosItemsYCaracteresFueraDeWinAnsiNoRompenElPdf() throws IOException {
        FacturaElectronica factura = factura(EstadoDte.APROBADO);
        for (int i = 0; i < 70; i++) {
            factura.getItems().add(item("Ítem " + i + " con una descripción bastante larga para forzar el corte en "
                    + "varias líneas ✓ 漢字", 1, "1000"));
        }

        try (PDDocument doc = Loader.loadPDF(generator.generar(factura))) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
        }
    }

    private FacturaElectronica factura(EstadoDte estado) {
        Establecimiento est = new Establecimiento();
        est.setCodigo("001");
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setCodigo("001");

        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-0");
        cliente.setRazonSocial("Distribuidora Sur SA");
        cliente.setCondicionIva(CondicionIva.RESPONSABLE_IVA);

        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(UUID.randomUUID());
        factura.setEstadoDte(estado);
        factura.setEstablecimiento(est);
        factura.setPuntoExpedicion(punto);
        factura.setNumeroDocumento(7);
        factura.setCondicionPago(CondicionPago.CONTADO);
        factura.setFechaEmision(Instant.parse("2026-10-29T13:00:00Z"));
        factura.setCliente(cliente);
        factura.getItems().add(item("Taladro percutor 220V", 2, "350000"));
        factura.setTotalIva10(new BigDecimal("63636"));
        factura.setTotalGeneral(new BigDecimal("700000"));
        return factura;
    }

    private static ItemFactura item(String descripcion, int cantidad, String precio) {
        ItemFactura item = new ItemFactura();
        item.setDescripcion(descripcion);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(new BigDecimal(precio));
        item.setTasaIva(TasaIva.DIEZ);
        item.setSubtotal(new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        return item;
    }

    private String extraerTexto(byte[] pdf) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }
}
