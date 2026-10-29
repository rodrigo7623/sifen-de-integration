package com.tottalstore.sifen.facturacion.kude;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.shared.TasaIva;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Arma el contenido del KuDE desde el XML firmado del DE o, si no lo hay, desde la factura. */
public final class KudeDatosFactory {

    private static final String NS_SIFEN = "http://ekuatia.set.gov.py/sifen/xsd";
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final ZoneId ZONA = ZoneId.of("America/Asuncion");

    private KudeDatosFactory() {
    }

    /** True si el texto es un rDE del SIFEN (el modo stub guarda un XML simplificado que no lo es). */
    public static boolean esDeSifen(String xml) {
        return xml != null && xml.contains("<rDE") && xml.contains(NS_SIFEN);
    }

    public static KudeDatos desdeXml(String xml) {
        Element de = parsear(xml);
        List<KudeDatos.Item> items = new ArrayList<>();
        NodeList nodos = de.getElementsByTagNameNS(NS_SIFEN, "gCamItem");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element it = (Element) nodos.item(i);
            BigDecimal total = num(texto(it, "dTotOpeItem"));
            boolean exento = "3".equals(texto(it, "iAfecIVA"));
            String tasa = texto(it, "dTasaIVA");
            items.add(new KudeDatos.Item(
                    texto(it, "dCodInt"),
                    texto(it, "dDesProSer"),
                    texto(it, "dDesUniMed"),
                    num(texto(it, "dCantProSer")),
                    num(texto(it, "dPUniProSer")),
                    num(texto(it, "dDescItem")),
                    exento ? total : BigDecimal.ZERO,
                    !exento && "5".equals(tasa) ? total : BigDecimal.ZERO,
                    !exento && "10".equals(tasa) ? total : BigDecimal.ZERO));
        }

        String dvRec = texto(de, "dDVRec");
        String receptorDoc = texto(de, "dRucRec") != null
                ? texto(de, "dRucRec") + (dvRec != null ? "-" + dvRec : "")
                : texto(de, "dNumIDRec");
        String direccionEmisor = unir(texto(de, "dDirEmi"), texto(de, "dNumCas"));
        // gCamFuFD (con el QR) es hermano del DE dentro del rDE, no hijo.
        String urlQr = texto(de.getOwnerDocument().getDocumentElement(), "dCarQR");
        String plazo = texto(de, "dPlazoCre");
        String cuotas = texto(de, "dCuotas");

        return new KudeDatos(
                texto(de, "dDesTiDE"),
                texto(de, "dNomEmi"),
                texto(de, "dNomFanEmi"),
                texto(de, "dRucEm") + "-" + texto(de, "dDVEmi"),
                texto(de, "dDesActEco"),
                direccionEmisor,
                texto(de, "dDesCiuEmi"),
                texto(de, "dTelEmi"),
                texto(de, "dEmailE"),
                texto(de, "dNumTim"),
                fecha(texto(de, "dFeIniT")),
                fecha(texto(de, "dFeFinT")),
                texto(de, "dEst") + "-" + texto(de, "dPunExp") + "-" + texto(de, "dNumDoc"),
                fechaHora(texto(de, "dFeEmiDE")),
                texto(de, "dDCondOpe"),
                plazo != null ? "Plazo: " + plazo : cuotas != null ? "Cuotas: " + cuotas : null,
                texto(de, "dDesMoneOpe"),
                texto(de, "dDesTipTra"),
                receptorDoc,
                texto(de, "dNomRec"),
                unir(texto(de, "dDirRec"), texto(de, "dNumCasRec")),
                texto(de, "dTelRec"),
                texto(de, "dEmailRec"),
                items,
                num(texto(de, "dSubExe")),
                num(texto(de, "dSub5")),
                num(texto(de, "dSub10")),
                num(texto(de, "dTotOpe")),
                num(texto(de, "dTotGralOpe")),
                num(texto(de, "dIVA5")),
                num(texto(de, "dIVA10")),
                num(texto(de, "dTotIVA")),
                de.getAttribute("Id"),
                urlQr,
                urlQr != null && urlQr.contains("consultas-test"),
                true);
    }

    /**
     * Vista previa sin XML firmado (borrador, o factura enviada en modo stub): los datos del emisor y
     * del timbrado salen de la configuración actual.
     */
    public static KudeDatos desdeFactura(FacturaElectronica f, ConfiguracionEmisor e) {
        List<KudeDatos.Item> items = new ArrayList<>();
        BigDecimal exe = BigDecimal.ZERO;
        BigDecimal sub5 = BigDecimal.ZERO;
        BigDecimal sub10 = BigDecimal.ZERO;
        int indice = 1;
        for (ItemFactura it : f.getItems()) {
            BigDecimal total = it.getSubtotal();
            TasaIva tasa = it.getTasaIva();
            items.add(new KudeDatos.Item(
                    it.getProducto() != null ? it.getProducto().getCodigo() : "ITEM-" + indice,
                    it.getDescripcion(),
                    "UNI",
                    BigDecimal.valueOf(it.getCantidad()),
                    it.getPrecioUnitario(),
                    BigDecimal.ZERO,
                    tasa == TasaIva.EXENTA ? total : BigDecimal.ZERO,
                    tasa == TasaIva.CINCO ? total : BigDecimal.ZERO,
                    tasa == TasaIva.DIEZ ? total : BigDecimal.ZERO));
            switch (tasa) {
                case EXENTA -> exe = exe.add(total);
                case CINCO -> sub5 = sub5.add(total);
                case DIEZ -> sub10 = sub10.add(total);
            }
            indice++;
        }

        boolean credito = f.getCondicionPago() == CondicionPago.CREDITO;
        String ruc = e.getRucBase() != null ? e.getRucBase() + (e.getDvRuc() != null ? "-" + e.getDvRuc() : "") : null;
        String cdc = f.getCdc() != null ? f.getCdc()
                : f.getRespuestaSifen() != null ? f.getRespuestaSifen().getCdc() : null;

        return new KudeDatos(
                "Factura electrónica",
                e.getRazonSocial(),
                e.getNombreFantasia(),
                ruc,
                e.getActividadEconomicaDescripcion(),
                unir(e.getDireccion(), e.getNumeroCasa()),
                e.getCiudadDescripcion(),
                e.getTelefono(),
                e.getEmail(),
                e.getTimbradoNumero(),
                e.getTimbradoFechaInicio() != null ? FECHA.format(e.getTimbradoFechaInicio()) : null,
                e.getTimbradoFechaFin() != null ? FECHA.format(e.getTimbradoFechaFin()) : null,
                f.numeroComprobante(),
                f.getFechaEmision() != null ? FECHA_HORA.format(f.getFechaEmision().atZone(ZONA)) : null,
                credito ? "Crédito" : "Contado",
                credito && f.getPlazoDias() != null ? "Plazo: " + f.getPlazoDias() + " días" : null,
                "Guarani",
                "Venta de mercadería",
                f.getCliente().getRuc(),
                f.getCliente().getRazonSocial(),
                f.getCliente().getDireccion(),
                null,
                f.getCliente().getEmail(),
                items,
                exe,
                sub5,
                sub10,
                f.getTotalGeneral(),
                f.getTotalGeneral(),
                f.getTotalIva5(),
                f.getTotalIva10(),
                f.getTotalIva5().add(f.getTotalIva10()),
                cdc,
                null,
                false,
                false);
    }

    private static Element parsear(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setExpandEntityReferences(false);
            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList des = doc.getElementsByTagNameNS(NS_SIFEN, "DE");
            if (des.getLength() == 0) {
                throw new IllegalArgumentException("El XML no contiene un elemento DE del SIFEN");
            }
            return (Element) des.item(0);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo leer el XML del DE", ex);
        }
    }

    /** Texto del primer descendiente con ese nombre local, o null si no existe o está vacío. */
    private static String texto(Element padre, String nombre) {
        NodeList l = padre.getElementsByTagNameNS(NS_SIFEN, nombre);
        if (l.getLength() == 0) {
            return null;
        }
        Node n = l.item(0);
        String t = n.getTextContent();
        return t == null || t.isBlank() ? null : t.trim();
    }

    private static BigDecimal num(String valor) {
        return valor != null ? new BigDecimal(valor) : BigDecimal.ZERO;
    }

    private static String fecha(String iso) {
        return iso != null ? FECHA.format(LocalDate.parse(iso)) : null;
    }

    private static String fechaHora(String iso) {
        return iso != null ? FECHA_HORA.format(LocalDateTime.parse(iso)) : null;
    }

    private static String unir(String a, String b) {
        if (a == null) {
            return b;
        }
        return b != null && !"0".equals(b) ? a + " " + b : a;
    }
}
