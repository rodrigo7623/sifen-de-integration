package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Renglon;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.TipoLibro;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Totales;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Exporta el Libro IVA Ventas o Compras a PDF (M4): A4 apaisado, con tipo de comprobante, timbrado, documento
 * asociado de las notas, paginado "Página X de N" y totales al final.
 *
 * <p>Cada columna tiene un ancho fijo y el texto que no entra se recorta con "…" (antes la condición
 * "Ajusta 001-001-0000001" de las notas se pisaba con el monto de la columna siguiente).
 */
@Component
public class LibroIvaPdfExporter {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDRectangle PAGINA = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    private static final float MARGEN = 36;
    private static final float ALTO_FILA = 12.5f;
    private static final float TAMANO = 7.5f;
    private static final float RELLENO = 3;

    private record Columna(String titulo, float ancho, boolean monto) {
    }

    private static final Columna[] COLUMNAS = {
        new Columna("Fecha", 44, false),
        new Columna("Tipo", 24, false),
        new Columna("Número", 68, false),
        new Columna("Timbrado", 42, false),
        new Columna("RUC/CI", 50, false),
        new Columna("Razón social", 110, false),
        new Columna("Condición", 44, false),
        new Columna("Doc. asociado", 68, false),
        new Columna("Grav. 10%", 54, true),
        new Columna("IVA 10%", 46, true),
        new Columna("Grav. 5%", 54, true),
        new Columna("IVA 5%", 46, true),
        new Columna("Exento", 50, true),
        new Columna("Total", 58, true),
    };

    private static final float ANCHO_TABLA;

    static {
        float ancho = 0;
        for (Columna c : COLUMNAS) {
            ancho += c.ancho();
        }
        ANCHO_TABLA = ancho;
    }

    public byte[] exportar(LibroIvaResponse libro) {
        try (PDDocument doc = new PDDocument()) {
            Pagina p = nuevaPagina(doc, libro);
            boolean sombreada = false;

            for (Renglon r : libro.renglones()) {
                if (p.y < MARGEN + 3 * ALTO_FILA) {
                    p.cs.close();
                    p = nuevaPagina(doc, libro);
                    sombreada = false;
                }
                if (sombreada) {
                    sombrear(p);
                }
                sombreada = !sombreada;
                fila(p, FONT,
                        FECHA.format(r.fecha()),
                        r.sigla(),
                        r.numero(),
                        r.timbrado(),
                        r.ruc(),
                        r.razonSocial(),
                        r.condicion(),
                        r.documentoAsociado() != null ? r.documentoAsociado() : "",
                        monto(r.gravado10()), monto(r.iva10()), monto(r.gravado5()), monto(r.iva5()),
                        monto(r.exento()), monto(r.total()));
            }
            if (libro.renglones().isEmpty()) {
                texto(p.cs, FONT, TAMANO + 1, MARGEN, p.y, libro.libro() == TipoLibro.VENTAS
                        ? "No hay comprobantes aprobados por el SIFEN ni ventas registradas en este período."
                        : "No hay compras imputadas al IVA registradas en este período.");
                p.y -= ALTO_FILA;
            }

            if (p.y < MARGEN + 6 * ALTO_FILA) {
                p.cs.close();
                p = nuevaPagina(doc, libro);
            }
            Totales t = libro.totales();
            linea(p.cs, p.y + ALTO_FILA - 3.5f, 0.8f);
            fila(p, FONT_BOLD, "Totales", "", "", "", "", t.cantidadComprobantes() + " comprobantes", "", "",
                    monto(t.gravado10()), monto(t.iva10()), monto(t.gravado5()), monto(t.iva5()),
                    monto(t.exento()), monto(t.total()));
            p.y -= ALTO_FILA / 2;
            texto(p.cs, FONT, 9, MARGEN, p.y, "Base imponible 10%: " + monto(t.baseImponible10())
                    + "      Base imponible 5%: " + monto(t.baseImponible5()));
            p.y -= ALTO_FILA + 1;
            texto(p.cs, FONT_BOLD, 9, MARGEN, p.y, libro.libro().etiquetaIva() + " del período: Gs. " + monto(t.totalIva()));
            p.y -= ALTO_FILA;
            texto(p.cs, FONT, 7, MARGEN, p.y,
                    "Los montos gravados incluyen el IVA. Las notas de crédito restan (montos negativos).");
            p.cs.close();

            numerarPaginas(doc);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF del " + libro.libro().titulo() + " " + libro.periodo(), e);
        }
    }

    private static final class Pagina {
        PDPageContentStream cs;
        float y;
    }

    private Pagina nuevaPagina(PDDocument doc, LibroIvaResponse libro) throws IOException {
        PDPage pagina = new PDPage(PAGINA);
        doc.addPage(pagina);
        Pagina p = new Pagina();
        p.cs = new PDPageContentStream(doc, pagina);
        p.y = PAGINA.getHeight() - MARGEN - 10;
        texto(p.cs, FONT_BOLD, 13, MARGEN, p.y, libro.libro().titulo() + " — período " + periodoLegible(libro.periodo()));
        p.y -= 15;
        texto(p.cs, FONT, 9, MARGEN, p.y, "Contribuyente: " + nvl(libro.emisorRazonSocial()) + "   ·   RUC " + nvl(libro.emisorRuc()));
        p.y -= 20;
        String[] titulos = java.util.Arrays.stream(COLUMNAS).map(Columna::titulo).toArray(String[]::new);
        titulos[4] = libro.libro() == TipoLibro.VENTAS ? "RUC/CI" : "RUC prov.";
        titulos[5] = libro.libro().columnaRazonSocial();
        fila(p, FONT_BOLD, titulos);
        linea(p.cs, p.y + ALTO_FILA - 3.5f, 0.6f);
        return p;
    }

    /** Pie de cada página, una vez que se sabe cuántas hay. */
    private void numerarPaginas(PDDocument doc) throws IOException {
        int total = doc.getNumberOfPages();
        String generado = "Generado el " + FECHA_HORA.format(LocalDateTime.now(ZoneId.of("America/Asuncion")));
        for (int i = 0; i < total; i++) {
            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, doc.getPage(i), PDPageContentStream.AppendMode.APPEND, true, true)) {
                String pagina = "Página " + (i + 1) + " de " + total;
                texto(cs, FONT, 7, MARGEN, MARGEN - 16, generado);
                texto(cs, FONT, 7, MARGEN + ANCHO_TABLA - ancho(FONT, 7, pagina), MARGEN - 16, pagina);
            }
        }
    }

    private void fila(Pagina p, PDType1Font font, String... valores) throws IOException {
        float x = MARGEN;
        for (int c = 0; c < COLUMNAS.length; c++) {
            Columna col = COLUMNAS[c];
            String v = recortar(font, valores[c] != null ? limpiar(valores[c]) : "", col.ancho() - 2 * RELLENO);
            float xTexto = col.monto() ? x + col.ancho() - RELLENO - ancho(font, TAMANO, v) : x + RELLENO;
            texto(p.cs, font, TAMANO, xTexto, p.y, v);
            x += col.ancho();
        }
        p.y -= ALTO_FILA;
    }

    private void sombrear(Pagina p) throws IOException {
        p.cs.setNonStrokingColor(0.95f, 0.96f, 0.98f);
        p.cs.addRect(MARGEN, p.y - 3.5f, ANCHO_TABLA, ALTO_FILA);
        p.cs.fill();
        p.cs.setNonStrokingColor(0f, 0f, 0f);
    }

    private void linea(PDPageContentStream cs, float y, float grosor) throws IOException {
        cs.setLineWidth(grosor);
        cs.moveTo(MARGEN, y);
        cs.lineTo(MARGEN + ANCHO_TABLA, y);
        cs.stroke();
    }

    private void texto(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String texto)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(limpiar(texto));
        cs.endText();
    }

    private static float ancho(PDType1Font font, float size, String texto) throws IOException {
        return font.getStringWidth(texto) / 1000 * size;
    }

    /** Recorta con "…" lo que no entra en el ancho de la columna. */
    private static String recortar(PDType1Font font, String texto, float maximo) throws IOException {
        if (ancho(font, TAMANO, texto) <= maximo) {
            return texto;
        }
        String recortado = texto;
        while (!recortado.isEmpty() && ancho(font, TAMANO, recortado + "…") > maximo) {
            recortado = recortado.substring(0, recortado.length() - 1);
        }
        return recortado.stripTrailing() + "…";
    }

    /**
     * Las fuentes estándar del PDF solo tienen los caracteres de WinAnsi: una razón social con, por
     * ejemplo, letras del guaraní (ẽ, g̃) haría fallar todo el reporte. Se reemplazan por la letra base.
     */
    static String limpiar(String texto) {
        StringBuilder sb = new StringBuilder(texto.length());
        for (char ch : texto.toCharArray()) {
            if (esCodificable(Character.toString(ch))) {
                sb.append(ch);
            } else {
                String base = Normalizer.normalize(Character.toString(ch), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                sb.append(!base.isEmpty() && esCodificable(base) ? base : "?");
            }
        }
        return sb.toString();
    }

    private static boolean esCodificable(String s) {
        try {
            FONT.encode(s);
            return true;
        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }

    private static String periodoLegible(String periodo) {
        try {
            java.time.YearMonth ym = java.time.YearMonth.parse(periodo);
            String mes = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.of("es"));
            return mes.substring(0, 1).toUpperCase(Locale.ROOT) + mes.substring(1) + " " + ym.getYear();
        } catch (RuntimeException e) {
            return periodo;
        }
    }

    private static String monto(BigDecimal valor) {
        return String.format(Locale.of("es", "PY"), "%,.0f", valor != null ? valor : BigDecimal.ZERO);
    }

    private static String nvl(String valor) {
        return valor != null ? valor : "-";
    }
}
