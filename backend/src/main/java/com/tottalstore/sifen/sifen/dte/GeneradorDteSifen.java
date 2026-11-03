package com.tottalstore.sifen.sifen.dte;

import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorRepository;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.firma.DteFirmado;
import com.tottalstore.sifen.firma.GeneradorDteService;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.CdcGenerator;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generación real del DE (Fases 3 y 4 de la integración al SIFEN): CDC con los datos reales de la
 * factura, XML según el Manual Técnico v150 ({@link DeXmlBuilder}), firma XMLDSig con el
 * certificado del contribuyente ({@link FirmadorXmlDsig}) y QR ({@link CodigoQrSifen}).
 *
 * <p>Los datos del emisor salen de la pantalla "Configuración del emisor" (tabla
 * {@code configuracion_emisor}) y el establecimiento/punto de expedición, de la propia factura.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class GeneradorDteSifen implements GeneradorDteService {

    static final ZoneId ZONA_PARAGUAY = ZoneId.of("America/Asuncion");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConfiguracionEmisorRepository configuracionEmisorRepository;
    private final CdcGenerator cdcGenerator;
    private final FirmadorXmlDsig firmador;
    private final DeXmlBuilder builder = new DeXmlBuilder();
    private final String idCsc;
    private final String csc;

    public GeneradorDteSifen(
            ConfiguracionEmisorRepository configuracionEmisorRepository,
            CdcGenerator cdcGenerator,
            PrivateKey sifenPrivateKey,
            X509Certificate sifenCertificado,
            @Value("${sifen.qr.id-csc}") String idCsc,
            @Value("${sifen.qr.csc}") String csc) {
        this.configuracionEmisorRepository = configuracionEmisorRepository;
        this.cdcGenerator = cdcGenerator;
        this.firmador = new FirmadorXmlDsig(sifenPrivateKey, sifenCertificado);
        this.idCsc = idCsc;
        this.csc = csc;
    }

    @Override
    public DteFirmado generar(FacturaElectronica factura, Ambiente ambiente) {
        ConfiguracionEmisor emisor = configuracionEmisorRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException("Falta cargar la configuración del emisor"));
        validarEmisor(emisor);

        LocalDateTime fechaEmision = LocalDateTime.ofInstant(factura.getFechaEmision(), ZONA_PARAGUAY).withNano(0);
        Instant ahora = Instant.now();
        LocalDateTime fechaFirma = LocalDateTime.ofInstant(ahora, ZONA_PARAGUAY).withNano(0);
        String codigoSeguridad = generarCodigoSeguridad();
        String cdc = cdcGenerator.generar(new CdcGenerator.CdcDatos(
                factura.getTipoDoc().codigoCdc(),
                emisor.getRucBase(),
                emisor.getDvRuc(),
                factura.getEstablecimiento().getCodigo(),
                factura.getPuntoExpedicion().getCodigo(),
                String.valueOf(factura.getNumeroDocumento()),
                emisor.getTipoContribuyente(),
                fechaEmision.toLocalDate(),
                1,
                codigoSeguridad));

        DeXmlBuilder.Resultado de = builder.construir(
                factura, emisor, ambiente, cdc, codigoSeguridad, fechaEmision, fechaFirma);
        Element rDE = de.documento().getDocumentElement();
        String digestValue = firmador.firmar(rDE, cdc);

        Element gCamFuFD = de.documento().createElementNS(DeXmlBuilder.NS_SIFEN, "gCamFuFD");
        Element dCarQR = de.documento().createElementNS(DeXmlBuilder.NS_SIFEN, "dCarQR");
        dCarQR.setTextContent(CodigoQrSifen.generar(
                ambiente,
                cdc,
                FECHA_HORA.format(fechaEmision),
                de.identificacionReceptor(),
                de.totalGeneral(),
                de.totalIva(),
                de.cantidadItems(),
                digestValue,
                idCsc,
                csc));
        gCamFuFD.appendChild(dCarQR);
        rDE.appendChild(gCamFuFD);

        return new DteFirmado(serializar(de.documento()), ahora, cdc, codigoSeguridad);
    }

    /** Campos del emisor obligatorios en el grupo D2 / C del DE. */
    static void validarEmisor(ConfiguracionEmisor e) {
        List<String> faltantes = new ArrayList<>();
        if (vacio(e.getRucBase()) || e.getDvRuc() == null) faltantes.add("RUC y dígito verificador");
        if (e.getTipoContribuyente() == null) faltantes.add("tipo de contribuyente");
        if (vacio(e.getRazonSocial())) faltantes.add("razón social");
        if (vacio(e.getDireccion())) faltantes.add("dirección");
        if (vacio(e.getDepartamentoCodigo()) || vacio(e.getDepartamentoDescripcion())) faltantes.add("departamento");
        if (vacio(e.getCiudadCodigo()) || vacio(e.getCiudadDescripcion())) faltantes.add("ciudad");
        if (vacio(e.getTelefono())) faltantes.add("teléfono");
        if (vacio(e.getEmail())) faltantes.add("email");
        if (vacio(e.getActividadEconomicaCodigo()) || vacio(e.getActividadEconomicaDescripcion())) {
            faltantes.add("actividad económica");
        }
        if (vacio(e.getTimbradoNumero()) || e.getTimbradoFechaInicio() == null) {
            faltantes.add("número y fecha de inicio del timbrado");
        }
        if (!faltantes.isEmpty()) {
            throw new BusinessException(
                    "Faltan datos en la configuración del emisor para generar el DE: " + String.join(", ", faltantes));
        }
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String generarCodigoSeguridad() {
        StringBuilder sb = new StringBuilder(9);
        for (int i = 0; i < 9; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /** Sin declaración XML ni indentación: el documento ya está firmado y no se puede reformatear. */
    static String serializar(Document documento) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(documento), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el DE firmado", e);
        }
    }
}
