package com.tottalstore.sifen.sifen.dte;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.facturacion.MotivoNota;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import com.tottalstore.sifen.shared.CondicionIva;
import com.tottalstore.sifen.shared.TasaIva;
import com.tottalstore.sifen.sifen.Ambiente;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Arma el XML del DE según el Manual Técnico del SIFEN v150, "Tabla de formato de campos de un DE"
 * (Schema XML 18, DE_v150.xsd). Cubre lo que emite hoy el sistema: factura electrónica (C002 = 1) de
 * venta de mercaderías en guaraníes, afectada al IVA, contado o crédito a plazo, y las notas de
 * crédito/débito (C002 = 5 / 6) que la ajustan.
 *
 * <p>Se construye con DOM (no concatenando strings) para que el contenido quede escapado y el
 * documento se pueda firmar directamente. El orden de los elementos sigue el del manual: el XSD
 * del SIFEN es una secuencia estricta.
 */
public class DeXmlBuilder {

    public static final String NS_SIFEN = "http://ekuatia.set.gov.py/sifen/xsd";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    /** D105: en el ambiente de prueba el nombre del emisor debe ser este literal. */
    static final String NOMBRE_EMISOR_TEST = "DE generado en ambiente de prueba - sin valor comercial ni fiscal";
    private static final int DECIMALES = 8;

    /** Datos de cálculo que el QR (sección 13.8) necesita leer del DE ya armado. */
    public record Resultado(Document documento, Element de, String totalGeneral, String totalIva,
            String identificacionReceptor, int cantidadItems) {}

    public Resultado construir(
            FacturaElectronica factura,
            ConfiguracionEmisor emisor,
            Ambiente ambiente,
            String cdc,
            String codigoSeguridad,
            LocalDateTime fechaEmision,
            LocalDateTime fechaFirma) {
        Document doc = nuevoDocumento();

        Element rDE = doc.createElementNS(NS_SIFEN, "rDE");
        // Mismo orden de atributos que el ejemplo del manual (7.2.2.1): si no se declara
        // explícitamente, el serializador agrega xmlns al final y el SIFEN responde 0160 "No se
        // informó el schema en el XML".
        rDE.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", NS_SIFEN);
        rDE.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", NS_XSI);
        rDE.setAttributeNS(NS_XSI, "xsi:schemaLocation", NS_SIFEN + " siRecepDE_v150.xsd");
        doc.appendChild(rDE);
        el(rDE, "dVerFor", "150");

        // A. Campos firmados del DE
        Element de = hijo(rDE, "DE");
        de.setAttribute("Id", cdc);
        de.setIdAttribute("Id", true);
        el(de, "dDVId", cdc.substring(cdc.length() - 1));
        el(de, "dFecFirma", FECHA_HORA.format(fechaFirma));
        el(de, "dSisFact", "1");

        // B. Operación
        Element gOpeDE = hijo(de, "gOpeDE");
        el(gOpeDE, "iTipEmi", "1");
        el(gOpeDE, "dDesTipEmi", "Normal");
        el(gOpeDE, "dCodSeg", codigoSeguridad);

        // C. Timbrado
        Establecimiento establecimiento = factura.getEstablecimiento();
        Element gTimb = hijo(de, "gTimb");
        TipoDocumento tipo = factura.getTipoDoc();
        el(gTimb, "iTiDE", String.valueOf(tipo.codigo()));
        el(gTimb, "dDesTiDE", tipo.descripcion());
        el(gTimb, "dNumTim", emisor.getTimbradoNumero());
        el(gTimb, "dEst", establecimiento.getCodigo());
        el(gTimb, "dPunExp", factura.getPuntoExpedicion().getCodigo());
        el(gTimb, "dNumDoc", String.format("%07d", factura.getNumeroDocumento()));
        el(gTimb, "dFeIniT", emisor.getTimbradoFechaInicio().toString());

        // D. Datos generales
        Element gDatGralOpe = hijo(de, "gDatGralOpe");
        el(gDatGralOpe, "dFeEmiDE", FECHA_HORA.format(fechaEmision));
        Element gOpeCom = hijo(gDatGralOpe, "gOpeCom");
        // D011/D012: solo en factura y autofactura (C002 = 1 o 4).
        if (tipo == TipoDocumento.FE) {
            el(gOpeCom, "iTipTra", "1");
            el(gOpeCom, "dDesTipTra", "Venta de mercadería");
        }
        el(gOpeCom, "iTImp", "1");
        el(gOpeCom, "dDesTImp", "IVA");
        el(gOpeCom, "cMoneOpe", "PYG");
        el(gOpeCom, "dDesMoneOpe", "Guarani");
        agregarEmisor(gDatGralOpe, emisor, establecimiento, ambiente);
        String identificacionReceptor = agregarReceptor(gDatGralOpe, factura.getCliente());

        // E. Campos específicos por tipo de documento: E1 (factura) o E5 (nota de crédito/débito)
        Element gDtipDE = hijo(de, "gDtipDE");
        Totales totales = new Totales();
        for (ItemFactura item : factura.getItems()) {
            totales.sumar(item);
        }
        if (tipo == TipoDocumento.FE) {
            Element gCamFE = hijo(gDtipDE, "gCamFE");
            el(gCamFE, "iIndPres", "1");
            el(gCamFE, "dDesIndPres", "Operación presencial");
            // E7: la condición de la operación solo se informa en factura/autofactura (rechazo 1501).
            agregarCondicion(gDtipDE, factura, totales.totalOperacion());
        } else {
            MotivoNota motivo = factura.getMotivoEmision();
            Element gCamNCDE = hijo(gDtipDE, "gCamNCDE");
            el(gCamNCDE, "iMotEmi", String.valueOf(motivo.codigo()));
            el(gCamNCDE, "dDesMotEmi", motivo.descripcion());
        }
        int indice = 0;
        for (ItemFactura item : factura.getItems()) {
            agregarItem(gDtipDE, item, ++indice);
        }

        // F. Totales
        Element gTotSub = hijo(de, "gTotSub");
        el(gTotSub, "dSubExe", num(totales.subExe));
        el(gTotSub, "dSubExo", "0");
        el(gTotSub, "dSub5", num(totales.sub5));
        el(gTotSub, "dSub10", num(totales.sub10));
        el(gTotSub, "dTotOpe", num(totales.totalOperacion()));
        el(gTotSub, "dTotDesc", "0");
        el(gTotSub, "dTotDescGlotem", "0");
        el(gTotSub, "dTotAntItem", "0");
        el(gTotSub, "dTotAnt", "0");
        el(gTotSub, "dPorcDescTotal", "0");
        el(gTotSub, "dDescTotal", "0");
        el(gTotSub, "dAnticipo", "0");
        el(gTotSub, "dRedon", "0");
        el(gTotSub, "dTotGralOpe", num(totales.totalOperacion()));
        el(gTotSub, "dIVA5", num(totales.iva5));
        el(gTotSub, "dIVA10", num(totales.iva10));
        el(gTotSub, "dLiqTotIVA5", "0");
        el(gTotSub, "dLiqTotIVA10", "0");
        el(gTotSub, "dTotIVA", num(totales.iva5.add(totales.iva10)));
        el(gTotSub, "dBaseGrav5", num(totales.base5));
        el(gTotSub, "dBaseGrav10", num(totales.base10));
        el(gTotSub, "dTBasGraIVA", num(totales.base5.add(totales.base10)));

        // H. Documento asociado: obligatorio en notas (2400), una sola vez (2415) y debe ser una FE
        // electrónica (2414). Va después de gTotSub (y del gCamGen opcional, que no se emite).
        if (tipo.esNota()) {
            Element gCamDEAsoc = hijo(de, "gCamDEAsoc");
            el(gCamDEAsoc, "iTipDocAso", "1");
            el(gCamDEAsoc, "dDesTipDocAso", "Electrónico");
            el(gCamDEAsoc, "dCdCDERef", factura.getFacturaAsociada().cdcVigente());
        }

        return new Resultado(doc, de, num(totales.totalOperacion()), num(totales.iva5.add(totales.iva10)),
                identificacionReceptor, factura.getItems().size());
    }

    /** D2: emisor. La dirección es la del establecimiento si la tiene cargada (D107 "dirección del
     * local donde se emite el DE"); si no, la del emisor. */
    private void agregarEmisor(Element padre, ConfiguracionEmisor emisor, Establecimiento est, Ambiente ambiente) {
        Element gEmis = hijo(padre, "gEmis");
        el(gEmis, "dRucEm", emisor.getRucBase());
        el(gEmis, "dDVEmi", String.valueOf(emisor.getDvRuc()));
        el(gEmis, "iTipCont", String.valueOf(emisor.getTipoContribuyente()));
        el(gEmis, "dNomEmi", ambiente == Ambiente.TEST ? NOMBRE_EMISOR_TEST : emisor.getRazonSocial());
        opcional(gEmis, "dNomFanEmi", emisor.getNombreFantasia());

        boolean usaEstablecimiento = tieneTexto(est.getDireccion());
        el(gEmis, "dDirEmi", usaEstablecimiento ? est.getDireccion() : emisor.getDireccion());
        el(gEmis, "dNumCas", valorODefecto(usaEstablecimiento ? est.getNumeroCasa() : emisor.getNumeroCasa(), "0"));
        opcional(gEmis, "dCompDir1", emisor.getComplementoDireccion1());
        opcional(gEmis, "dCompDir2", emisor.getComplementoDireccion2());
        el(gEmis, "cDepEmi", usaEstablecimiento ? est.getDepartamentoCodigo() : emisor.getDepartamentoCodigo());
        el(gEmis, "dDesDepEmi", usaEstablecimiento ? est.getDepartamentoDescripcion() : emisor.getDepartamentoDescripcion());
        String distrito = usaEstablecimiento ? est.getDistritoCodigo() : emisor.getDistritoCodigo();
        if (tieneTexto(distrito)) {
            el(gEmis, "cDisEmi", distrito);
            el(gEmis, "dDesDisEmi", usaEstablecimiento ? est.getDistritoDescripcion() : emisor.getDistritoDescripcion());
        }
        el(gEmis, "cCiuEmi", usaEstablecimiento ? est.getCiudadCodigo() : emisor.getCiudadCodigo());
        el(gEmis, "dDesCiuEmi", usaEstablecimiento ? est.getCiudadDescripcion() : emisor.getCiudadDescripcion());
        el(gEmis, "dTelEmi", valorODefecto(est.getTelefono(), emisor.getTelefono()));
        el(gEmis, "dEmailE", valorODefecto(est.getEmail(), emisor.getEmail()));
        Element gActEco = hijo(gEmis, "gActEco");
        el(gActEco, "cActEco", emisor.getActividadEconomicaCodigo());
        el(gActEco, "dDesActEco", emisor.getActividadEconomicaDescripcion());
    }

    /** D3: receptor. Devuelve la identificación que va en el QR (D206 o D210). */
    private String agregarReceptor(Element padre, Cliente cliente) {
        Element gDatRec = hijo(padre, "gDatRec");
        boolean contribuyente = cliente.getCondicionIva() != CondicionIva.CONSUMIDOR_FINAL;
        String[] ruc = cliente.getRuc().trim().split("-");
        String base = ruc[0];

        el(gDatRec, "iNatRec", contribuyente ? "1" : "2");
        el(gDatRec, "iTiOpe", contribuyente ? "1" : "2");
        el(gDatRec, "cPaisRec", "PRY");
        el(gDatRec, "dDesPaisRe", "Paraguay");
        if (contribuyente) {
            el(gDatRec, "iTiContRec", tipoContribuyenteReceptor(base));
            el(gDatRec, "dRucRec", base);
            el(gDatRec, "dDVRec", ruc.length > 1 ? ruc[1] : "0");
        } else {
            el(gDatRec, "iTipIDRec", "1");
            el(gDatRec, "dDTipIDRec", "Cédula paraguaya");
            el(gDatRec, "dNumIDRec", base);
        }
        el(gDatRec, "dNomRec", cliente.getRazonSocial());
        opcional(gDatRec, "dEmailRec", cliente.getEmail());
        return base;
    }

    /** Convención de la SET: los RUC de personas jurídicas empiezan en 80.000.000; los de personas
     * físicas coinciden con su número de cédula. */
    static String tipoContribuyenteReceptor(String rucBase) {
        try {
            return Long.parseLong(rucBase) >= 80_000_000L ? "2" : "1";
        } catch (NumberFormatException e) {
            return "1";
        }
    }

    /** E7: condición de la operación. */
    private void agregarCondicion(Element gDtipDE, FacturaElectronica factura, BigDecimal total) {
        Element gCamCond = hijo(gDtipDE, "gCamCond");
        if (factura.getCondicionPago() == CondicionPago.CREDITO) {
            el(gCamCond, "iCondOpe", "2");
            el(gCamCond, "dDCondOpe", "Crédito");
            Element gPagCred = hijo(gCamCond, "gPagCred");
            el(gPagCred, "iCondCred", "1");
            el(gPagCred, "dDCondCred", "Plazo");
            el(gPagCred, "dPlazoCre", factura.getPlazoDias() + " días");
        } else {
            el(gCamCond, "iCondOpe", "1");
            el(gCamCond, "dDCondOpe", "Contado");
            Element gPaConEIni = hijo(gCamCond, "gPaConEIni");
            el(gPaConEIni, "iTiPago", "1");
            el(gPaConEIni, "dDesTiPag", "Efectivo");
            el(gPaConEIni, "dMonTiPag", num(total));
            el(gPaConEIni, "cMoneTiPag", "PYG");
            el(gPaConEIni, "dDMoneTiPag", "Guarani");
        }
    }

    /** E8: ítem, con su valor (E8.1) y su IVA (E8.2). */
    private void agregarItem(Element gDtipDE, ItemFactura item, int indice) {
        Element gCamItem = hijo(gDtipDE, "gCamItem");
        el(gCamItem, "dCodInt", item.getProducto() != null ? recortar(item.getProducto().getCodigo(), 20) : "ITEM-" + indice);
        el(gCamItem, "dDesProSer", recortar(item.getDescripcion(), 120));
        el(gCamItem, "cUniMed", "77");
        el(gCamItem, "dDesUniMed", "UNI");
        el(gCamItem, "dCantProSer", String.valueOf(item.getCantidad()));

        BigDecimal total = totalItem(item);
        Element gValorItem = hijo(gCamItem, "gValorItem");
        el(gValorItem, "dPUniProSer", num(item.getPrecioUnitario()));
        el(gValorItem, "dTotBruOpeItem", num(total));
        Element gValorRestaItem = hijo(gValorItem, "gValorRestaItem");
        el(gValorRestaItem, "dDescItem", "0");
        el(gValorRestaItem, "dDescGloItem", "0");
        el(gValorRestaItem, "dAntPreUniIt", "0");
        el(gValorRestaItem, "dAntGloPreUniIt", "0");
        el(gValorRestaItem, "dTotOpeItem", num(total));

        Iva iva = Iva.de(item.getTasaIva(), total);
        Element gCamIVA = hijo(gCamItem, "gCamIVA");
        el(gCamIVA, "iAfecIVA", iva.exento ? "3" : "1");
        el(gCamIVA, "dDesAfecIVA", iva.exento ? "Exento" : "Gravado IVA");
        el(gCamIVA, "dPropIVA", iva.exento ? "0" : "100");
        el(gCamIVA, "dTasaIVA", String.valueOf(item.getTasaIva().porcentaje()));
        el(gCamIVA, "dBasGravIVA", num(iva.base));
        el(gCamIVA, "dLiqIVAItem", num(iva.liquidacion));
        // E737 dBasExe: no está en el manual v150 publicado, lo exige el XSD vigente del SIFEN
        // (rechazo 0160 "Elemento esperado: dBasExe dentro de: gCamIVA"). Solo es distinto de 0
        // para ítems "gravado parcial" (E731 = 4), que el sistema no emite.
        el(gCamIVA, "dBasExe", "0");
    }

    static BigDecimal totalItem(ItemFactura item) {
        return item.getPrecioUnitario().multiply(BigDecimal.valueOf(item.getCantidad()));
    }

    /** E735/E736: base gravada = total / (1 + tasa) y liquidación = base × tasa, a 8 decimales. */
    record Iva(boolean exento, BigDecimal base, BigDecimal liquidacion) {
        static Iva de(TasaIva tasa, BigDecimal total) {
            if (tasa == TasaIva.EXENTA) {
                return new Iva(true, BigDecimal.ZERO, BigDecimal.ZERO);
            }
            BigDecimal base = total.divide(BigDecimal.ONE.add(tasa.fraccion()), DECIMALES, RoundingMode.HALF_UP);
            BigDecimal liquidacion = base.multiply(tasa.fraccion()).setScale(DECIMALES, RoundingMode.HALF_UP);
            return new Iva(false, base, liquidacion);
        }
    }

    private static final class Totales {
        BigDecimal subExe = BigDecimal.ZERO;
        BigDecimal sub5 = BigDecimal.ZERO;
        BigDecimal sub10 = BigDecimal.ZERO;
        BigDecimal iva5 = BigDecimal.ZERO;
        BigDecimal iva10 = BigDecimal.ZERO;
        BigDecimal base5 = BigDecimal.ZERO;
        BigDecimal base10 = BigDecimal.ZERO;

        void sumar(ItemFactura item) {
            BigDecimal total = totalItem(item);
            Iva iva = Iva.de(item.getTasaIva(), total);
            switch (item.getTasaIva()) {
                case EXENTA -> subExe = subExe.add(total);
                case CINCO -> {
                    sub5 = sub5.add(total);
                    iva5 = iva5.add(iva.liquidacion);
                    base5 = base5.add(iva.base);
                }
                case DIEZ -> {
                    sub10 = sub10.add(total);
                    iva10 = iva10.add(iva.liquidacion);
                    base10 = base10.add(iva.base);
                }
            }
        }

        BigDecimal totalOperacion() {
            return subExe.add(sub5).add(sub10);
        }
    }

    /** Números sin ceros decimales sobrantes ni notación científica (11000.00 → "11000"). */
    static String num(BigDecimal valor) {
        if (valor == null || valor.signum() == 0) return "0";
        return valor.stripTrailingZeros().toPlainString();
    }

    private static Document nuevoDocumento() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("No se pudo crear el documento XML del DE", e);
        }
    }

    private static Element hijo(Element padre, String nombre) {
        Element e = padre.getOwnerDocument().createElementNS(NS_SIFEN, nombre);
        padre.appendChild(e);
        return e;
    }

    private static void el(Element padre, String nombre, String valor) {
        hijo(padre, nombre).setTextContent(valor);
    }

    private static void opcional(Element padre, String nombre, String valor) {
        if (tieneTexto(valor)) {
            el(padre, nombre, valor.trim());
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String valorODefecto(String valor, String defecto) {
        return tieneTexto(valor) ? valor : defecto;
    }

    private static String recortar(String texto, int max) {
        return texto.length() > max ? texto.substring(0, max) : texto;
    }
}
