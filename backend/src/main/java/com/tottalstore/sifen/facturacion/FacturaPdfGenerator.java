package com.tottalstore.sifen.facturacion;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
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
 * Genera el PDF de una factura ya confirmada (RF-10, REP-06): datos del cliente, ítems, montos de
 * IVA y, si el DTE fue aprobado, el CDC devuelto por el SIFEN. Sin estado, fácil de testear.
 */
@Component
public class FacturaPdfGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final float MARGEN_IZQ = 50;
    private static final float ANCHO_PAGINA = PDRectangle.A4.getWidth() - 2 * MARGEN_IZQ;

    public byte[] generar(FacturaElectronica factura) {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                float y = PDRectangle.A4.getHeight() - 60;

                y = titulo(cs, y, "Factura Electrónica — Tottal Store");
                y -= 10;
                y = linea(cs, y, FONT, "Documento: " + factura.getTipoDoc() + "  ·  Estado: "
                        + factura.getEstadoDte());
                y = linea(cs, y, FONT, "Fecha de emisión: "
                        + FECHA.format(factura.getFechaEmision().atZone(ZoneId.systemDefault())));
                if (factura.getRespuestaSifen() != null && factura.getRespuestaSifen().getCdc() != null) {
                    y = linea(cs, y, FONT, "CDC: " + factura.getRespuestaSifen().getCdc());
                }
                y -= 10;

                y = subtitulo(cs, y, "Cliente");
                y = linea(cs, y, FONT, "RUC/CI: " + safe(factura.getCliente().getRuc()));
                y = linea(cs, y, FONT, "Razón social: " + safe(factura.getCliente().getRazonSocial()));
                y -= 10;

                y = subtitulo(cs, y, "Ítems");
                y = filaItems(cs, y, "Descripción", "Cant.", "Precio unit.", "Subtotal", true);
                for (ItemFactura item : factura.getItems()) {
                    y = filaItems(
                            cs,
                            y,
                            item.getDescripcion(),
                            String.valueOf(item.getCantidad()),
                            moneda(item.getPrecioUnitario()),
                            moneda(item.getSubtotal()),
                            false);
                }
                y -= 10;

                y = subtitulo(cs, y, "Totales");
                y = linea(cs, y, FONT, "IVA 5%: " + moneda(factura.getTotalIva5()));
                y = linea(cs, y, FONT, "IVA 10%: " + moneda(factura.getTotalIva10()));
                y = linea(cs, y, FONT_BOLD, "Total general: " + moneda(factura.getTotalGeneral()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el PDF de la factura " + factura.getId(), e);
        }
    }

    private float titulo(PDPageContentStream cs, float y, String texto) throws IOException {
        return escribir(cs, y, FONT_BOLD, 16, texto);
    }

    private float subtitulo(PDPageContentStream cs, float y, String texto) throws IOException {
        return escribir(cs, y, FONT_BOLD, 12, texto);
    }

    private float linea(PDPageContentStream cs, float y, PDType1Font font, String texto) throws IOException {
        return escribir(cs, y, font, 11, texto);
    }

    private float escribir(PDPageContentStream cs, float y, PDType1Font font, int size, String texto)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(MARGEN_IZQ, y);
        cs.showText(texto);
        cs.endText();
        return y - (size + 8);
    }

    private float filaItems(
            PDPageContentStream cs, float y, String desc, String cant, String precio, String subtotal, boolean header)
            throws IOException {
        PDType1Font font = header ? FONT_BOLD : FONT;
        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(MARGEN_IZQ, y);
        cs.showText(truncar(desc, 40));
        cs.endText();

        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(MARGEN_IZQ + ANCHO_PAGINA * 0.55f, y);
        cs.showText(cant);
        cs.endText();

        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(MARGEN_IZQ + ANCHO_PAGINA * 0.68f, y);
        cs.showText(precio);
        cs.endText();

        cs.beginText();
        cs.setFont(font, 10);
        cs.newLineAtOffset(MARGEN_IZQ + ANCHO_PAGINA * 0.85f, y);
        cs.showText(subtotal);
        cs.endText();

        return y - 16;
    }

    private String truncar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }

    private String moneda(java.math.BigDecimal valor) {
        return String.format(Locale.of("es", "PY"), "%,.0f Gs.", valor);
    }

    private String safe(String valor) {
        return valor != null ? valor : "-";
    }
}
