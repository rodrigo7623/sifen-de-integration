package com.tottalstore.sifen.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaRepository;
import com.tottalstore.sifen.facturacion.FacturaService;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.shared.TasaIva;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibroIvaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ConfiguracionEmisorService configuracionEmisorService;

    private LibroIvaService service;

    @BeforeEach
    void setUp() {
        service = new LibroIvaService(facturaRepository, configuracionEmisorService);
        ConfiguracionEmisor emisor = new ConfiguracionEmisor();
        emisor.setRucBase("4309652");
        emisor.setDvRuc(2);
        emisor.setRazonSocial("BARTOLOME RAMIREZ MEDINA");
        emisor.setTimbradoNumero("12345678");
        when(configuracionEmisorService.obtenerOCrear()).thenReturn(emisor);
    }

    @Test
    void separaMontosPorTasaYSumaTotales() {
        FacturaElectronica mixta = factura(7, "2026-10-05T10:00:00-03:00",
                item(2, "110000", TasaIva.DIEZ), item(1, "21000", TasaIva.CINCO), item(1, "5000", TasaIva.EXENTA));
        FacturaElectronica solo10 = factura(8, "2026-10-20T15:30:00-03:00", item(1, "550000", TasaIva.DIEZ));
        solo10.setCondicionPago(CondicionPago.CREDITO);
        when(facturaRepository.findEmitidasEntre(any(), any(), any())).thenReturn(List.of(mixta, solo10));

        LibroIvaResponse libro = service.generar(YearMonth.of(2026, 10));

        assertThat(libro.emisorRuc()).isEqualTo("4309652-2");
        assertThat(libro.renglones()).hasSize(2);
        LibroIvaResponse.Renglon r = libro.renglones().get(0);
        assertThat(r.numero()).isEqualTo("001-002-0000007");
        assertThat(r.timbrado()).isEqualTo("12345678");
        assertThat(r.condicion()).isEqualTo("Contado");
        assertThat(r.gravado10()).isEqualByComparingTo("220000");
        assertThat(r.iva10()).isEqualByComparingTo("20000");
        assertThat(r.gravado5()).isEqualByComparingTo("21000");
        assertThat(r.iva5()).isEqualByComparingTo("1000");
        assertThat(r.exento()).isEqualByComparingTo("5000");
        assertThat(r.total()).isEqualByComparingTo("246000");
        assertThat(libro.renglones().get(1).condicion()).isEqualTo("Crédito");

        LibroIvaResponse.Totales t = libro.totales();
        assertThat(t.cantidadComprobantes()).isEqualTo(2);
        assertThat(t.gravado10()).isEqualByComparingTo("770000");
        assertThat(t.iva10()).isEqualByComparingTo("70000");
        assertThat(t.baseImponible10()).isEqualByComparingTo("700000");
        assertThat(t.baseImponible5()).isEqualByComparingTo("20000");
        assertThat(t.totalIva()).isEqualByComparingTo("71000");
        assertThat(t.total()).isEqualByComparingTo("796000");
    }

    @Test
    void consultaElMesEnHoraDeAsuncionYSoloAprobadasOAnuladas() {
        when(facturaRepository.findEmitidasEntre(any(), any(), any())).thenReturn(List.of());

        service.generar(YearMonth.of(2026, 10));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<EstadoDte>> estados = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(facturaRepository).findEmitidasEntre(
                estados.capture(),
                eq(ZonedDateTime.parse("2026-10-01T00:00:00-03:00").toInstant()),
                eq(ZonedDateTime.parse("2026-11-01T00:00:00-03:00").toInstant()));
        assertThat(estados.getValue()).containsExactlyInAnyOrder(EstadoDte.APROBADO, EstadoDte.ANULADO);
    }

    @Test
    void fechaDelRenglonEsLaDeAsuncionAunqueEnUtcSeaOtroDia() {
        // 31/10 22:00 en Asunción = 01/11 01:00 UTC: pertenece a octubre.
        FacturaElectronica f = factura(1, "2026-10-31T22:00:00-03:00", item(1, "11000", TasaIva.DIEZ));
        when(facturaRepository.findEmitidasEntre(any(), any(), any())).thenReturn(List.of(f));

        assertThat(service.generar(YearMonth.of(2026, 10)).renglones().get(0).fecha())
                .isEqualTo(java.time.LocalDate.of(2026, 10, 31));
    }

    @Test
    void laNotaDeCreditoRestaYLaFacturaAnuladaSigueListada() {
        FacturaElectronica factura = factura(5, "2026-10-05T10:00:00-03:00", item(1, "110000", TasaIva.DIEZ));
        factura.setEstadoDte(EstadoDte.ANULADO);
        FacturaElectronica nc = factura(1, "2026-10-06T10:00:00-03:00", item(1, "110000", TasaIva.DIEZ));
        nc.setTipoDoc(TipoDocumento.NC);
        nc.setCondicionPago(null);
        nc.setFacturaAsociada(factura);
        FacturaElectronica nd = factura(1, "2026-10-07T10:00:00-03:00", item(1, "22000", TasaIva.DIEZ));
        nd.setTipoDoc(TipoDocumento.ND);
        nd.setCondicionPago(null);
        nd.setFacturaAsociada(factura);
        when(facturaRepository.findEmitidasEntre(any(), any(), any())).thenReturn(List.of(factura, nc, nd));

        LibroIvaResponse libro = service.generar(YearMonth.of(2026, 10));

        LibroIvaResponse.Renglon renglonNc = libro.renglones().get(1);
        assertThat(renglonNc.tipoComprobante()).isEqualTo("Nota de crédito electrónica");
        assertThat(renglonNc.condicion()).isEqualTo("Ajusta 001-002-0000005");
        assertThat(renglonNc.gravado10()).isEqualByComparingTo("-110000");
        assertThat(renglonNc.iva10()).isEqualByComparingTo("-10000");
        assertThat(renglonNc.total()).isEqualByComparingTo("-110000");
        assertThat(libro.renglones().get(2).total()).isEqualByComparingTo("22000");
        // La factura y su NC se compensan: queda solo la nota de débito.
        assertThat(libro.totales().cantidadComprobantes()).isEqualTo(3);
        assertThat(libro.totales().iva10()).isEqualByComparingTo("2000");
        assertThat(libro.totales().total()).isEqualByComparingTo("22000");
    }

    @Test
    void exportaExcelConUnaFilaPorComprobanteYTotales() throws Exception {
        FacturaElectronica f = factura(3, "2026-10-05T10:00:00-03:00", item(1, "110000", TasaIva.DIEZ));
        when(facturaRepository.findEmitidasEntre(any(), any(), any())).thenReturn(List.of(f));
        LibroIvaResponse libro = service.generar(YearMonth.of(2026, 10));

        byte[] xlsx = new LibroIvaExcelExporter().exportar(libro);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet hoja = wb.getSheetAt(0);
            assertThat(hoja.getRow(3).getCell(0).getStringCellValue()).isEqualTo("Fecha");
            assertThat(hoja.getRow(4).getCell(2).getStringCellValue()).isEqualTo("001-002-0000003");
            assertThat(hoja.getRow(4).getCell(7).getNumericCellValue()).isEqualTo(110000.0);
            assertThat(hoja.getRow(4).getCell(8).getNumericCellValue()).isEqualTo(10000.0);
            assertThat(hoja.getRow(5).getCell(0).getStringCellValue()).startsWith("Totales (1");
        }
    }

    @Test
    void exportaPdfPaginadoConTotales() throws Exception {
        List<FacturaElectronica> muchas = new java.util.ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            muchas.add(factura(i, "2026-10-05T10:00:00-03:00", item(1, "11000", TasaIva.DIEZ)));
        }
        when(facturaRepository.findEmitidasEntre(any(), any(), any())).thenReturn(muchas);
        LibroIvaResponse libro = service.generar(YearMonth.of(2026, 10));

        byte[] pdf = new LibroIvaPdfExporter().exportar(libro);

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(1);
            String texto = new PDFTextStripper().getText(doc);
            assertThat(texto).contains("Libro IVA Ventas").contains("001-002-0000060").contains("660.000");
        }
    }

    private static FacturaElectronica factura(int numero, String fecha, ItemFactura... items) {
        Establecimiento est = new Establecimiento();
        est.setCodigo("001");
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setCodigo("002");
        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-6");
        cliente.setRazonSocial("CLIENTE DE PRUEBA S.A.");

        FacturaElectronica f = new FacturaElectronica();
        f.setId(java.util.UUID.randomUUID());
        f.setEstablecimiento(est);
        f.setPuntoExpedicion(punto);
        f.setNumeroDocumento(numero);
        f.setCliente(cliente);
        f.setCondicionPago(CondicionPago.CONTADO);
        f.setEstadoDte(EstadoDte.APROBADO);
        f.setFechaEmision(Instant.from(ZonedDateTime.parse(fecha)));
        for (ItemFactura i : items) {
            f.getItems().add(i);
        }
        calcularTotales(f);
        return f;
    }

    private static void calcularTotales(FacturaElectronica f) {
        try {
            var m = FacturaService.class.getDeclaredMethod("calcularTotales", FacturaElectronica.class);
            m.setAccessible(true);
            m.invoke(null, f);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ItemFactura item(int cantidad, String precio, TasaIva tasa) {
        ItemFactura i = new ItemFactura();
        i.setDescripcion("Ítem");
        i.setCantidad(cantidad);
        i.setPrecioUnitario(new BigDecimal(precio));
        i.setTasaIva(tasa);
        i.setSubtotal(new BigDecimal(precio).multiply(BigDecimal.valueOf(cantidad)));
        return i;
    }
}
