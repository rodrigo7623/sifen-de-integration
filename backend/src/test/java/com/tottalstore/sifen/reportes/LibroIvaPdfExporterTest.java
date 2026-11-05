package com.tottalstore.sifen.reportes;

import static org.assertj.core.api.Assertions.assertThat;

import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Origen;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Renglon;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.TipoLibro;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Totales;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class LibroIvaPdfExporterTest {

    private final LibroIvaPdfExporter exporter = new LibroIvaPdfExporter();

    private static Renglon renglon(String tipo, String numero, String razonSocial, String condicion, String total) {
        BigDecimal t = new BigDecimal(total);
        BigDecimal iva = t.divide(new BigDecimal("11"), 0, java.math.RoundingMode.HALF_UP);
        boolean nota = condicion.startsWith("Ajusta ");
        return new Renglon(UUID.randomUUID(), null, Origen.SIFEN, LocalDate.of(2026, 11, 2), tipo,
                tipo.startsWith("Nota de crédito") ? "NC" : "FE", numero, "04309652", "01" + "0".repeat(42),
                "80016096-7", razonSocial, nota ? "-" : condicion, nota ? condicion.substring(7) : null,
                t, iva, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, t);
    }

    private static LibroIvaResponse libro(List<Renglon> renglones) {
        Totales totales = new Totales(renglones.size(), BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.TEN);
        return new LibroIvaResponse(TipoLibro.VENTAS, "2026-11", "4309652-2", "TOTTAL STORE", renglones, totales);
    }

    private static String texto(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void separaTipoNumeroYDocumentoAsociadoDeLasNotas() throws Exception {
        byte[] pdf = exporter.exportar(libro(List.of(
                renglon("Factura electrónica", "001-001-0000001", "RETAIL S.A.", "Contado", "11000"),
                renglon("Nota de crédito electrónica", "001-001-0000001", "RETAIL S.A.", "Ajusta 001-001-0000001", "-5500"))));

        String texto = texto(pdf);
        assertThat(texto).contains("Tipo", "Timbrado", "Doc. asociado", "Noviembre 2026", "Página 1 de 1");
        assertThat(texto).contains("NC 001-001-0000001");
        // Antes el documento asociado y el monto quedaban pegados: "001-001-0000001-5.500".
        assertThat(texto).doesNotContain("0000001-5.500");
    }

    @Test
    void paginaConElTotalDePaginas() throws Exception {
        List<Renglon> renglones = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            renglones.add(renglon("Factura electrónica", String.format("001-001-%07d", i), "Cliente " + i, "Contado", "11000"));
        }

        String texto = texto(exporter.exportar(libro(renglones)));

        assertThat(texto).contains("Página 1 de 2", "Página 2 de 2", "50 comprobantes", "001-001-0000050", "04309652");
    }

    @Test
    void unaRazonSocialConLetrasFueraDeLaFuenteNoRompeElReporteYSeRecortaLaQueNoEntra() throws Exception {
        String larga = "Comercial Ñandutí y Asociados de Mbaracayú Sociedad Anónima Importadora y Exportadora";
        byte[] pdf = exporter.exportar(libro(List.of(
                renglon("Factura electrónica", "001-001-0000001", "Kuña Pirapẽ g̃uasu", "Contado", "11000"),
                renglon("Factura electrónica", "001-001-0000002", larga, "Contado", "11000"))));

        String texto = texto(pdf);
        assertThat(texto).contains("Kuña Pirape").contains("Comercial Ñandutí").contains("…");
        assertThat(texto).doesNotContain("Exportadora");
    }

    @Test
    void periodoSinComprobantes() throws Exception {
        assertThat(texto(exporter.exportar(libro(List.of())))).contains("No hay comprobantes aprobados");
    }
}
