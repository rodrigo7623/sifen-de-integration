package com.tottalstore.sifen.facturacion;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.kude.KudeDatos;
import com.tottalstore.sifen.facturacion.kude.KudeDatosFactory;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

/**
 * Genera el KuDE (representación gráfica del DE, sección 13 del Manual Técnico v150) de una factura
 * (RF-10, REP-06): encabezado con emisor y timbrado, número de factura, datos de la operación y del
 * receptor, ítems con su valor de venta por tasa, subtotales, liquidación del IVA, CDC y código QR.
 *
 * <p>Si la factura tiene XML firmado del SIFEN, todo sale de ese XML (el manual prohíbe que el KuDE
 * muestre datos que no estén en el DE firmado). Si no (borrador o modo stub), se arma una vista
 * previa con los datos de la factura y se marca como sin validez.
 */
@Component
public class FacturaPdfGenerator {

    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDRectangle PAGINA = PDRectangle.A4;
    private static final float MARGEN = 36;
    private static final float ANCHO = PAGINA.getWidth() - 2 * MARGEN;
    private static final float X_FIN = MARGEN + ANCHO;
    private static final Color GRIS = new Color(0xE8, 0xEE, 0xF6);
    private static final Color ROJO = new Color(0xB0, 0x1E, 0x1E);
    private static final String URL_CONSULTA_PROD = "https://ekuatia.set.gov.py/consultas/";
    private static final String URL_CONSULTA_TEST = "https://ekuatia.set.gov.py/consultas-test/";

    /** Columnas de ítems: x de inicio de cada una y fin de la última. Las numéricas se alinean a la derecha. */
    private static final float[] COL = {MARGEN, 86, 236, 266, 296, 356, 391, 447, 503, X_FIN};
    private static final String[] COL_TITULOS = {"Código", "Descripción", "Unidad", "Cant.", "Precio unit.", "Desc.", "Exentas", "5%", "10%"};
    private static final int PRIMERA_NUMERICA = 3;

    private final ConfiguracionEmisorService configuracionEmisorService;

    public FacturaPdfGenerator(ConfiguracionEmisorService configuracionEmisorService) {
        this.configuracionEmisorService = configuracionEmisorService;
    }

    public byte[] generar(FacturaElectronica factura) {
        String xml = factura.getRespuestaSifen() != null ? factura.getRespuestaSifen().getXmlFirmado() : null;
        KudeDatos datos = KudeDatosFactory.esDeSifen(xml)
                ? KudeDatosFactory.desdeXml(xml)
                : KudeDatosFactory.desdeFactura(factura, configuracionEmisorService.obtenerOCrear());
        return generar(datos, factura.getEstadoDte());
    }

    byte[] generar(KudeDatos d, EstadoDte estado) {
        try (PDDocument doc = new PDDocument()) {
            Lienzo l = new Lienzo(doc);
            l.nuevaPagina();

            if (estado != EstadoDte.APROBADO || !d.desdeXmlFirmado()) {
                aviso(l, d, estado);
            }
            encabezado(l, d);
            datosOperacion(l, d);
            items(l, d);
            totales(l, d);
            consulta(l, d);
            l.cerrar();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el KuDE de la factura " + d.numero(), e);
        }
    }

    // --- Secciones -------------------------------------------------------------------------------

    private void aviso(Lienzo l, KudeDatos d, EstadoDte estado) throws IOException {
        String texto = estado == EstadoDte.APROBADO
                ? "VISTA PREVIA: documento sin XML firmado del SIFEN. Sin validez fiscal."
                : "DOCUMENTO NO APROBADO POR EL SIFEN (estado " + estado + "). Sin validez fiscal.";
        l.cs.setNonStrokingColor(ROJO);
        texto(l.cs, FONT_BOLD, 10, MARGEN, l.y, texto);
        l.cs.setNonStrokingColor(Color.BLACK);
        l.y -= 16;
    }

    private void encabezado(Lienzo l, KudeDatos d) throws IOException {
        float alto = 112;
        float top = l.y;
        float division = MARGEN + ANCHO * 0.58f;
        rect(l.cs, MARGEN, top - alto, ANCHO, alto);
        linea(l.cs, division, top, division, top - alto);

        float y = top - 16;
        // En TEST la razón social es el literal largo que exige el manual: se parte en dos líneas.
        for (String renglon : envolver(nvl(d.emisorRazonSocial()), FONT_BOLD, 10, division - MARGEN - 16, 2)) {
            texto(l.cs, FONT_BOLD, 10, MARGEN + 8, y, renglon);
            y -= 13;
        }
        y -= 1;
        for (String renglon : new String[] {
            d.emisorNombreFantasia(),
            d.emisorActividad(),
            d.emisorDireccion(),
            d.emisorCiudad() != null ? "Ciudad: " + d.emisorCiudad() : null,
            unirConSeparador(d.emisorTelefono() != null ? "Tel.: " + d.emisorTelefono() : null, d.emisorEmail())
        }) {
            if (renglon != null) {
                texto(l.cs, FONT, 8.5f, MARGEN + 8, y, ajustar(renglon, FONT, 8.5f, division - MARGEN - 16));
                y -= 12;
            }
        }

        float xd = division + 10;
        y = top - 16;
        texto(l.cs, FONT, 9, xd, y, "RUC: " + nvl(d.emisorRuc()));
        y -= 13;
        texto(l.cs, FONT, 9, xd, y, "Timbrado N° " + nvl(d.timbrado()));
        y -= 13;
        texto(l.cs, FONT, 9, xd, y, "Fecha de inicio de vigencia: " + nvl(d.timbradoInicio()));
        y -= 13;
        if (d.timbradoFin() != null) {
            texto(l.cs, FONT, 9, xd, y, "Fecha de fin de vigencia: " + d.timbradoFin());
            y -= 13;
        }
        y -= 4;
        texto(l.cs, FONT_BOLD, 11, xd, y, nvl(d.tipoDocumento()).toUpperCase(Locale.ROOT));
        y -= 16;
        texto(l.cs, FONT_BOLD, 13, xd, y, "N° " + (d.numero() != null ? d.numero() : "(sin número)"));

        l.y = top - alto - 8;
    }

    private void datosOperacion(Lienzo l, KudeDatos d) throws IOException {
        List<String[]> izq = new ArrayList<>();
        izq.add(new String[] {"Fecha y hora de emisión: ", d.fechaEmision()});
        izq.add(new String[] {"Condición de venta: ", unirConSeparador(d.condicion(), d.detalleCredito())});
        izq.add(new String[] {"Moneda: ", d.moneda()});
        izq.add(new String[] {"Tipo de transacción: ", d.tipoTransaccion()});
        List<String[]> der = new ArrayList<>();
        der.add(new String[] {"RUC/Documento de identidad N°: ", d.receptorDocumento()});
        der.add(new String[] {"Nombre o razón social: ", d.receptorNombre()});
        if (d.receptorDireccion() != null) {
            der.add(new String[] {"Dirección: ", d.receptorDireccion()});
        }
        if (d.receptorTelefono() != null) {
            der.add(new String[] {"Teléfono: ", d.receptorTelefono()});
        }
        if (d.receptorEmail() != null) {
            der.add(new String[] {"Correo electrónico: ", d.receptorEmail()});
        }

        int filas = Math.max(izq.size(), der.size());
        float alto = filas * 12 + 12;
        float top = l.y;
        float division = MARGEN + ANCHO * 0.42f;
        rect(l.cs, MARGEN, top - alto, ANCHO, alto);
        etiquetas(l.cs, izq, MARGEN + 8, top - 14, division - MARGEN - 12);
        etiquetas(l.cs, der, division, top - 14, X_FIN - division - 8);
        l.y = top - alto - 10;
    }

    private void items(Lienzo l, KudeDatos d) throws IOException {
        encabezadoItems(l);
        for (KudeDatos.Item it : d.items()) {
            List<String> descripcion = envolver(nvl(it.descripcion()), FONT, 8, COL[2] - COL[1] - 6, 3);
            float alto = descripcion.size() * 10 + 4;
            if (l.y - alto < MARGEN + 40) {
                l.nuevaPagina();
                encabezadoItems(l);
            }
            float y = l.y - 9;
            texto(l.cs, FONT, 8, COL[0] + 3, y, ajustar(nvl(it.codigo()), FONT, 8, COL[1] - COL[0] - 6));
            for (int i = 0; i < descripcion.size(); i++) {
                texto(l.cs, FONT, 8, COL[1] + 3, y - i * 10, descripcion.get(i));
            }
            texto(l.cs, FONT, 8, COL[2] + 3, y, nvl(it.unidad()));
            String[] numeros = {
                cantidad(it.cantidad()), monto(it.precioUnitario()), monto(it.descuento()),
                monto(it.exentas()), monto(it.gravado5()), monto(it.gravado10())
            };
            for (int i = 0; i < numeros.length; i++) {
                derecha(l.cs, FONT, 8, COL[PRIMERA_NUMERICA + i + 1] - 3, y, numeros[i]);
            }
            l.y -= alto;
            l.cs.setStrokingColor(GRIS);
            linea(l.cs, MARGEN, l.y, X_FIN, l.y);
            l.cs.setStrokingColor(Color.BLACK);
        }
        l.y -= 6;
    }

    private void encabezadoItems(Lienzo l) throws IOException {
        float alto = 16;
        l.cs.setNonStrokingColor(GRIS);
        l.cs.addRect(MARGEN, l.y - alto, ANCHO, alto);
        l.cs.fill();
        l.cs.setNonStrokingColor(Color.BLACK);
        float y = l.y - 11;
        for (int i = 0; i < COL_TITULOS.length; i++) {
            if (i >= PRIMERA_NUMERICA) {
                derecha(l.cs, FONT_BOLD, 7.5f, COL[i + 1] - 3, y, COL_TITULOS[i]);
            } else {
                texto(l.cs, FONT_BOLD, 7.5f, COL[i] + 3, y, COL_TITULOS[i]);
            }
        }
        texto(l.cs, FONT, 6.5f, COL[6] + 3, l.y + 3, "Valor de venta");
        l.y -= alto;
    }

    private void totales(Lienzo l, KudeDatos d) throws IOException {
        float alto = 62;
        if (l.y - alto < MARGEN + 120) {
            l.nuevaPagina();
        }
        float top = l.y;
        rect(l.cs, MARGEN, top - alto, ANCHO, alto);

        float y = top - 13;
        texto(l.cs, FONT_BOLD, 8.5f, MARGEN + 8, y, "SUBTOTAL:");
        derecha(l.cs, FONT_BOLD, 8.5f, COL[7] - 3, y, monto(d.subtotalExentas()));
        derecha(l.cs, FONT_BOLD, 8.5f, COL[8] - 3, y, monto(d.subtotal5()));
        derecha(l.cs, FONT_BOLD, 8.5f, COL[9] - 3, y, monto(d.subtotal10()));
        y -= 14;
        texto(l.cs, FONT_BOLD, 9, MARGEN + 8, y, "TOTAL DE LA OPERACIÓN:");
        derecha(l.cs, FONT_BOLD, 9, X_FIN - 3, y, monto(d.totalOperacion()));
        y -= 14;
        texto(l.cs, FONT, 8.5f, MARGEN + 8, y, "TOTAL EN GUARANÍES:");
        derecha(l.cs, FONT, 8.5f, X_FIN - 3, y, monto(d.totalGuaranies()));
        y -= 14;
        texto(l.cs, FONT, 8.5f, MARGEN + 8, y, "LIQUIDACIÓN IVA:   (5%) " + monto(d.liquidacionIva5())
                + "     (10%) " + monto(d.liquidacionIva10()));
        derecha(l.cs, FONT_BOLD, 8.5f, X_FIN - 3, y, "TOTAL IVA: " + monto(d.totalIva()));
        l.y = top - alto - 10;
    }

    private void consulta(Lienzo l, KudeDatos d) throws IOException {
        float lado = 92; // ~32 mm: el manual pide como mínimo 25 mm incluido el margen
        float top = l.y;
        float xTexto = MARGEN;
        if (d.urlQr() != null) {
            qr(l.cs, d.urlQr(), MARGEN, top - lado, lado);
            xTexto = MARGEN + lado + 12;
        }
        float ancho = X_FIN - xTexto;
        float y = top - 16;
        String tipo = d.tipoDocumento() != null ? d.tipoDocumento() : "Factura electrónica";
        for (String renglon : envolver("Consulte la validez de esta " + tipo
                + " con el número de CDC impreso abajo en:", FONT, 8.5f, ancho, 2)) {
            texto(l.cs, FONT, 8.5f, xTexto, y, renglon);
            y -= 11;
        }
        texto(l.cs, FONT, 8.5f, xTexto, y, d.ambienteTest() || !d.desdeXmlFirmado() ? URL_CONSULTA_TEST : URL_CONSULTA_PROD);
        y -= 18;
        texto(l.cs, FONT_BOLD, 10, xTexto, y, "CDC: " + (d.cdc() != null ? d.cdcAgrupado() : "(sin CDC)"));
        y -= 18;
        texto(l.cs, FONT_BOLD, 7.5f, xTexto, y, "ESTE DOCUMENTO ES UNA REPRESENTACIÓN GRÁFICA DE UN DOCUMENTO ELECTRÓNICO (XML)");
        l.y = Math.min(y, top - lado) - 8;
    }

    // --- Primitivas ------------------------------------------------------------------------------

    /** Página actual y posición vertical; abre páginas nuevas cuando los ítems no entran. */
    private static final class Lienzo {
        final PDDocument doc;
        PDPageContentStream cs;
        float y;

        Lienzo(PDDocument doc) {
            this.doc = doc;
        }

        void nuevaPagina() throws IOException {
            cerrar();
            PDPage pagina = new PDPage(PAGINA);
            doc.addPage(pagina);
            cs = new PDPageContentStream(doc, pagina);
            y = PAGINA.getHeight() - MARGEN;
        }

        void cerrar() throws IOException {
            if (cs != null) {
                cs.close();
                cs = null;
            }
        }
    }

    private void qr(PDPageContentStream cs, String contenido, float x, float y, float lado) throws IOException {
        BitMatrix m;
        try {
            m = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.MARGIN, 2,
                    EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IOException("No se pudo generar el código QR", e);
        }
        // Se dibuja como vector (un cuadrado por módulo): nítido a cualquier escala y sin AWT.
        float modulo = lado / m.getWidth();
        cs.setNonStrokingColor(Color.BLACK);
        for (int fila = 0; fila < m.getHeight(); fila++) {
            for (int col = 0; col < m.getWidth(); col++) {
                if (m.get(col, fila)) {
                    cs.addRect(x + col * modulo, y + lado - (fila + 1) * modulo, modulo, modulo);
                }
            }
        }
        cs.fill();
    }

    private void etiquetas(PDPageContentStream cs, List<String[]> pares, float x, float y, float ancho) throws IOException {
        for (String[] par : pares) {
            texto(cs, FONT_BOLD, 8.5f, x, y, par[0]);
            float w = ancho(par[0], FONT_BOLD, 8.5f);
            texto(cs, FONT, 8.5f, x + w, y, ajustar(nvl(par[1]), FONT, 8.5f, ancho - w));
            y -= 12;
        }
    }

    private static void rect(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.setLineWidth(0.6f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void linea(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.setLineWidth(0.6f);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static void texto(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String texto)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(seguro(texto, font));
        cs.endText();
    }

    private static void derecha(PDPageContentStream cs, PDType1Font font, float size, float xFin, float y, String texto)
            throws IOException {
        texto(cs, font, size, xFin - ancho(texto, font, size), y, texto);
    }

    private static float ancho(String texto, PDType1Font font, float size) throws IOException {
        return font.getStringWidth(seguro(texto, font)) / 1000 * size;
    }

    /** Las fuentes estándar de PDF solo codifican WinAnsi: otros caracteres se reemplazan por '?'. */
    private static String seguro(String texto, PDType1Font font) {
        StringBuilder sb = new StringBuilder(texto.length());
        texto.codePoints().forEach(cp -> {
            String c = new String(Character.toChars(cp));
            try {
                font.encode(c);
                sb.append(c);
            } catch (IOException | IllegalArgumentException e) {
                sb.append('?');
            }
        });
        return sb.toString();
    }

    private static String ajustar(String texto, PDType1Font font, float size, float maximo) throws IOException {
        if (ancho(texto, font, size) <= maximo) {
            return texto;
        }
        String t = texto;
        while (t.length() > 1 && ancho(t + "…", font, size) > maximo) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }

    private static List<String> envolver(String texto, PDType1Font font, float size, float maximo, int maxLineas)
            throws IOException {
        List<String> lineas = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split("\\s+")) {
            String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
            if (ancho(candidato, font, size) <= maximo || actual.isEmpty()) {
                actual = new StringBuilder(candidato);
            } else {
                lineas.add(actual.toString());
                actual = new StringBuilder(palabra);
            }
        }
        lineas.add(actual.toString());
        if (lineas.size() > maxLineas) {
            List<String> recortadas = new ArrayList<>(lineas.subList(0, maxLineas));
            recortadas.set(maxLineas - 1, ajustar(recortadas.get(maxLineas - 1) + " " + lineas.get(maxLineas), font, size, maximo));
            return recortadas;
        }
        for (int i = 0; i < lineas.size(); i++) {
            lineas.set(i, ajustar(lineas.get(i), font, size, maximo));
        }
        return lineas;
    }

    private static String monto(BigDecimal valor) {
        return String.format(Locale.of("es", "PY"), "%,.0f", valor != null ? valor : BigDecimal.ZERO);
    }

    private static String cantidad(BigDecimal valor) {
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static String unirConSeparador(String a, String b) {
        if (a == null) {
            return b;
        }
        return b != null ? a + " · " + b : a;
    }

    private static String nvl(String valor) {
        return valor != null ? valor : "-";
    }
}
