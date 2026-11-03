package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaRepository;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Renglon;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Totales;
import com.tottalstore.sifen.shared.TasaIva;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Libro IVA Ventas mensual (M4, Release 3), armado a partir de los DE aprobados por el SIFEN:
 * facturas y notas de crédito/débito (M6). Las facturas ANULADAS también se listan: la anulación se
 * hace con una nota de crédito, que es la que resta, no la desaparición de la factura original.
 */
@Service
public class LibroIvaService {

    /** Huso de la fecha de emisión del DE (igual que GeneradorDteSifen): define a qué mes pertenece. */
    static final ZoneId ZONA = ZoneId.of("America/Asuncion");

    private final FacturaRepository facturaRepository;
    private final ConfiguracionEmisorService configuracionEmisorService;

    public LibroIvaService(
            FacturaRepository facturaRepository, ConfiguracionEmisorService configuracionEmisorService) {
        this.facturaRepository = facturaRepository;
        this.configuracionEmisorService = configuracionEmisorService;
    }

    public LibroIvaResponse generar(YearMonth periodo) {
        Instant desde = periodo.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant hasta = periodo.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();
        List<FacturaElectronica> facturas = facturaRepository.findEmitidasEntre(
                EnumSet.of(EstadoDte.APROBADO, EstadoDte.ANULADO), desde, hasta);

        ConfiguracionEmisor emisor = configuracionEmisorService.obtenerOCrear();
        // El timbrado no se guarda por factura: se toma el vigente de la configuración del emisor.
        String timbrado = emisor.getTimbradoNumero();

        List<Renglon> renglones = facturas.stream().map(f -> renglon(f, timbrado)).toList();
        return new LibroIvaResponse(
                periodo.toString(), rucEmisor(emisor), emisor.getRazonSocial(), renglones, totales(renglones));
    }

    /** Las notas de crédito van con montos negativos: restan del débito fiscal del mes en que se
     * emiten, aunque la factura que ajustan sea de otro mes. Las de débito suman como una factura. */
    static Renglon renglon(FacturaElectronica factura, String timbrado) {
        BigDecimal signo = factura.getTipoDoc() == TipoDocumento.NC ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        BigDecimal gravado10 = sumarItems(factura, TasaIva.DIEZ);
        BigDecimal gravado5 = sumarItems(factura, TasaIva.CINCO);
        BigDecimal exento = sumarItems(factura, TasaIva.EXENTA);
        return new Renglon(
                factura.getId(),
                factura.getFechaEmision().atZone(ZONA).toLocalDate(),
                factura.getTipoDoc().descripcion(),
                numeroComprobante(factura),
                timbrado,
                factura.cdcVigente(),
                factura.getCliente().getRuc(),
                factura.getCliente().getRazonSocial(),
                condicion(factura),
                gravado10.multiply(signo),
                // Se reutiliza el IVA ya calculado por ítem al confirmar, para que el libro coincida
                // centavo a centavo con lo informado al SIFEN en cada DE.
                factura.getTotalIva10().multiply(signo),
                gravado5.multiply(signo),
                factura.getTotalIva5().multiply(signo),
                exento.multiply(signo),
                factura.getTotalGeneral().multiply(signo));
    }

    private static String condicion(FacturaElectronica factura) {
        if (factura.esNota()) {
            String asociada = factura.getFacturaAsociada() != null
                    ? factura.getFacturaAsociada().numeroComprobante() : null;
            return asociada != null ? "Ajusta " + asociada : "-";
        }
        return factura.getCondicionPago() == CondicionPago.CREDITO ? "Crédito" : "Contado";
    }

    static Totales totales(List<Renglon> renglones) {
        BigDecimal gravado10 = sumar(renglones, Renglon::gravado10);
        BigDecimal iva10 = sumar(renglones, Renglon::iva10);
        BigDecimal gravado5 = sumar(renglones, Renglon::gravado5);
        BigDecimal iva5 = sumar(renglones, Renglon::iva5);
        return new Totales(
                renglones.size(),
                gravado10,
                iva10,
                gravado10.subtract(iva10),
                gravado5,
                iva5,
                gravado5.subtract(iva5),
                sumar(renglones, Renglon::exento),
                iva10.add(iva5),
                sumar(renglones, Renglon::total));
    }

    /** Número de comprobante con el formato impreso del KuDE: 001-001-0000001. */
    static String numeroComprobante(FacturaElectronica factura) {
        String numero = factura.numeroComprobante();
        return numero != null ? numero : "-";
    }

    private static BigDecimal sumarItems(FacturaElectronica factura, TasaIva tasa) {
        return factura.getItems().stream()
                .filter(i -> i.getTasaIva() == tasa)
                .map(ItemFactura::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal sumar(List<Renglon> renglones, java.util.function.Function<Renglon, BigDecimal> campo) {
        return renglones.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String rucEmisor(ConfiguracionEmisor emisor) {
        if (emisor.getRucBase() == null) {
            return null;
        }
        return emisor.getDvRuc() != null ? emisor.getRucBase() + "-" + emisor.getDvRuc() : emisor.getRucBase();
    }
}
