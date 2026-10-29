package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Renglon;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Totales;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/** Exporta el Libro IVA Ventas a PDF (M4): A4 apaisado, con paginado y totales al final. */
@Component
public class LibroIvaPdfExporter {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDRectangle PAGINA = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGEN = 36;
    private static final float ALTO_FILA = 14;
    private static final float TAMANO = 8;

    private static final String[] COLUMNAS = {
        "Fecha", "Número", "RUC/CI", "Razón social", "Cond.", "Grav. 10%", "IVA 10%", "Grav. 5%", "IVA 5%", "Exento", "Total"
    };
    /** Posición x de cada columna; las de montos (desde la 5) se alinean a la derecha de la siguiente. */
    private static final float[] X = {MARGEN, 86, 170, 240, 410, 450, 520, 580, 650, 705, 760, PAGINA.getWidth() - MARGEN};
    private static final int PRIMERA_COLUMNA_MONTO = 5;

    public byte[] exportar(LibroIvaResponse libro) {
        try (PDDocument doc = new PDDocument()) {
            Pagina p = nuevaPagina(doc, libro, 1);
            int numeroPagina = 1;

            for (Renglon r : libro.renglones()) {
                if (p.y < MARGEN + 3 * ALTO_FILA) {
                    p.cs.close();
                    p = nuevaPagina(doc, libro, ++numeroPagina);
                }
                fila(p, FONT, FECHA.format(r.fecha()), r.numero(), r.clienteRuc(), truncar(r.clienteRazonSocial(), 34),
                        r.condicion(), monto(r.gravado10()), monto(r.iva10()), monto(r.gravado5()), monto(r.iva5()),
                        monto(r.exento()), monto(r.total()));
            }

            if (p.y < MARGEN + 6 * ALTO_FILA) {
                p.cs.close();
                p = nuevaPagina(doc, libro, ++numeroPagina);
            }
            Totales t = libro.totales();
            p.cs.moveTo(MARGEN, p.y + ALTO_FILA - 3);
            p.cs.lineTo(PAGINA.getWidth() - MARGEN, p.y + ALTO_FILA - 3);
            p.cs.stroke();
            fila(p, FONT_BOLD, "Totales", t.cantidadComprobantes() + " comprob.", "", "", "", monto(t.gravado10()),
                    monto(t.iva10()), monto(t.gravado5()), monto(t.iva5()), monto(t.exento()), monto(t.total()));
            p.y -= ALTO_FILA;
            texto(p.cs, FONT, 9, MARGEN, p.y, "Base imponible 10%: " + monto(t.baseImponible10())
                    + "    Base imponible 5%: " + monto(t.baseImponible5()));
            p.y -= ALTO_FILA;
            texto(p.cs, FONT_BOLD, 9, MARGEN, p.y, "IVA débito fiscal del período: " + monto(t.totalIva()) + " Gs.");
            p.cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF del Libro IVA " + libro.periodo(), e);
        }
    }

    private static final class Pagina {
        PDPageContentStream cs;
        float y;
    }

    private Pagina nuevaPagina(PDDocument doc, LibroIvaResponse libro, int numero) throws IOException {
        PDPage pagina = new PDPage(PAGINA);
        doc.addPage(pagina);
        Pagina p = new Pagina();
        p.cs = new PDPageContentStream(doc, pagina);
        p.y = PAGINA.getHeight() - MARGEN - 10;
        texto(p.cs, FONT_BOLD, 13, MARGEN, p.y, "Libro IVA Ventas — período " + libro.periodo());
        texto(p.cs, FONT, 8, PAGINA.getWidth() - MARGEN - 50, p.y, "Página " + numero);
        p.y -= 16;
        texto(p.cs, FONT, 9, MARGEN, p.y, "Contribuyente: " + nvl(libro.emisorRazonSocial()) + "  ·  RUC "
                + nvl(libro.emisorRuc()));
        p.y -= 22;
        fila(p, FONT_BOLD, COLUMNAS);
        return p;
    }

    private void fila(Pagina p, PDType1Font font, String... valores) throws IOException {
        for (int c = 0; c < valores.length; c++) {
            String v = valores[c] != null ? valores[c] : "";
            float x = X[c];
            if (c >= PRIMERA_COLUMNA_MONTO) {
                x = X[c + 1] - 4 - font.getStringWidth(v) / 1000 * TAMANO;
            }
            texto(p.cs, font, TAMANO, x, p.y, v);
        }
        p.y -= ALTO_FILA;
    }

    private void texto(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String texto)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(texto);
        cs.endText();
    }

    private static String monto(BigDecimal valor) {
        return String.format(Locale.of("es", "PY"), "%,.0f", valor);
    }

    private static String truncar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }

    private static String nvl(String valor) {
        return valor != null ? valor : "-";
    }
}
