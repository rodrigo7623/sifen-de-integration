package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaRepository;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import com.tottalstore.sifen.registro.ComprobanteRegistrado;
import com.tottalstore.sifen.registro.ComprobanteRegistradoRepository;
import com.tottalstore.sifen.registro.TipoComprobanteRg90;
import com.tottalstore.sifen.registro.TipoRegistro;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Origen;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Renglon;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.TipoLibro;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Totales;
import com.tottalstore.sifen.reportes.dto.LiquidacionIvaResponse;
import com.tottalstore.sifen.shared.TasaIva;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Libros IVA mensuales (M4, Release 3).
 *
 * <ul>
 *   <li><b>Ventas</b> (débito fiscal): los DE aprobados por el SIFEN (facturas y notas de M6) más las
 *       ventas cargadas a mano con comprobantes preimpresos o tickets. Las facturas ANULADAS también
 *       se listan: la anulación se hace con una nota de crédito, que es la que resta.
 *   <li><b>Compras</b> (crédito fiscal): los comprobantes de compra registrados que se imputan al IVA.
 * </ul>
 */
@Service
public class LibroIvaService {

    /** Huso de la fecha de emisión del DE (igual que GeneradorDteSifen): define a qué mes pertenece. */
    static final ZoneId ZONA = ZoneId.of("America/Asuncion");

    private static final Comparator<Renglon> ORDEN =
            Comparator.comparing(Renglon::fecha).thenComparing(r -> r.numero() != null ? r.numero() : "");

    private final FacturaRepository facturaRepository;
    private final ComprobanteRegistradoRepository comprobanteRepository;
    private final ConfiguracionEmisorService configuracionEmisorService;

    public LibroIvaService(
            FacturaRepository facturaRepository,
            ComprobanteRegistradoRepository comprobanteRepository,
            ConfiguracionEmisorService configuracionEmisorService) {
        this.facturaRepository = facturaRepository;
        this.comprobanteRepository = comprobanteRepository;
        this.configuracionEmisorService = configuracionEmisorService;
    }

    /** Libro IVA Ventas (se mantiene el nombre del método de R3). */
    public LibroIvaResponse generar(YearMonth periodo) {
        return generar(periodo, TipoLibro.VENTAS);
    }

    public LibroIvaResponse generar(YearMonth periodo, TipoLibro libro) {
        ConfiguracionEmisor emisor = configuracionEmisorService.obtenerOCrear();
        List<Renglon> renglones = new ArrayList<>();

        if (libro == TipoLibro.VENTAS) {
            Instant desde = periodo.atDay(1).atStartOfDay(ZONA).toInstant();
            Instant hasta = periodo.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();
            // El timbrado no se guarda por factura: se toma el vigente de la configuración del emisor.
            String timbrado = emisor.getTimbradoNumero();
            facturaRepository.findEmitidasEntre(EnumSet.of(EstadoDte.APROBADO, EstadoDte.ANULADO), desde, hasta)
                    .forEach(f -> renglones.add(renglon(f, timbrado)));
        }

        TipoRegistro registro = libro == TipoLibro.VENTAS ? TipoRegistro.VENTA : TipoRegistro.COMPRA;
        comprobanteRepository.delPeriodo(registro, periodo.atDay(1), periodo.plusMonths(1).atDay(1)).stream()
                // Al Libro IVA Compras solo va lo imputado al IVA (lo del IRE/IRP va solo al archivo RG 90).
                .filter(c -> libro == TipoLibro.VENTAS || c.isImputaIva())
                .forEach(c -> renglones.add(renglon(c)));

        renglones.sort(ORDEN);
        return new LibroIvaResponse(
                libro, periodo.toString(), rucEmisor(emisor), emisor.getRazonSocial(), renglones, totales(renglones));
    }

    /** Débito (Libro Ventas) menos crédito (Libro Compras), y cuántos comprobantes van al archivo RG 90. */
    public LiquidacionIvaResponse liquidacion(YearMonth periodo) {
        LibroIvaResponse ventas = generar(periodo, TipoLibro.VENTAS);
        LibroIvaResponse compras = generar(periodo, TipoLibro.COMPRAS);
        List<ComprobanteRegistrado> registrados =
                comprobanteRepository.delPeriodo(periodo.atDay(1), periodo.plusMonths(1).atDay(1));
        int electronicos = (int) registrados.stream().filter(ComprobanteRegistrado::esElectronico).count();

        Totales v = ventas.totales();
        Totales c = compras.totales();
        return new LiquidacionIvaResponse(
                periodo.toString(),
                v.cantidadComprobantes(), v.total(), v.iva10(), v.iva5(), v.totalIva(),
                c.cantidadComprobantes(), c.total(), c.iva10(), c.iva5(), c.totalIva(),
                v.totalIva().subtract(c.totalIva()),
                registrados.size() - electronicos,
                electronicos);
    }

    /** Las notas de crédito van con montos negativos: restan del débito fiscal del mes en que se
     * emiten, aunque la factura que ajustan sea de otro mes. Las de débito suman como una factura. */
    static Renglon renglon(FacturaElectronica factura, String timbrado) {
        BigDecimal signo = factura.getTipoDoc() == TipoDocumento.NC ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        BigDecimal gravado10 = sumarItems(factura, TasaIva.DIEZ);
        BigDecimal gravado5 = sumarItems(factura, TasaIva.CINCO);
        BigDecimal exento = sumarItems(factura, TasaIva.EXENTA);
        String asociado = factura.esNota() && factura.getFacturaAsociada() != null
                ? factura.getFacturaAsociada().numeroComprobante() : null;
        return new Renglon(
                factura.getId(),
                null,
                Origen.SIFEN,
                factura.getFechaEmision().atZone(ZONA).toLocalDate(),
                factura.getTipoDoc().descripcion(),
                siglaElectronica(factura.getTipoDoc()),
                numeroComprobante(factura),
                timbrado,
                factura.cdcVigente(),
                factura.getCliente().getRuc(),
                factura.getCliente().getRazonSocial(),
                condicion(factura),
                asociado,
                gravado10.multiply(signo),
                // Se reutiliza el IVA ya calculado por ítem al confirmar, para que el libro coincida
                // centavo a centavo con lo informado al SIFEN en cada DE.
                factura.getTotalIva10().multiply(signo),
                gravado5.multiply(signo),
                factura.getTotalIva5().multiply(signo),
                exento.multiply(signo),
                factura.getTotalGeneral().multiply(signo));
    }

    static Renglon renglon(ComprobanteRegistrado c) {
        BigDecimal signo = c.getTipoComprobante() == TipoComprobanteRg90.NOTA_CREDITO
                ? BigDecimal.ONE.negate() : BigDecimal.ONE;
        boolean nota = c.getTipoComprobante().esNota();
        return new Renglon(
                null,
                c.getId(),
                c.esElectronico() ? Origen.DE_RECIBIDO : Origen.REGISTRADO,
                c.getFechaEmision(),
                c.getTipoComprobante().descripcion() + (c.esElectronico() ? " electrónica" : ""),
                c.getTipoComprobante().sigla(),
                c.getNumero() != null ? c.getNumero() : "-",
                c.getTimbrado(),
                c.getCdc(),
                c.getIdentificacion(),
                c.getRazonSocial() != null ? c.getRazonSocial() : "-",
                nota ? "-" : (c.getCondicion() == CondicionPago.CREDITO ? "Crédito" : "Contado"),
                nota ? c.getAsociadoNumero() : null,
                c.getGravado10().multiply(signo),
                c.getIva10().multiply(signo),
                c.getGravado5().multiply(signo),
                c.getIva5().multiply(signo),
                c.getExento().multiply(signo),
                c.getTotal().multiply(signo));
    }

    private static String condicion(FacturaElectronica factura) {
        if (factura.esNota()) {
            return "-";
        }
        return factura.getCondicionPago() == CondicionPago.CREDITO ? "Crédito" : "Contado";
    }

    private static String siglaElectronica(TipoDocumento tipo) {
        return switch (tipo) {
            case NC -> "NC";
            case ND -> "ND";
            default -> "FE";
        };
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

    static String rucEmisor(ConfiguracionEmisor emisor) {
        if (emisor.getRucBase() == null) {
            return null;
        }
        return emisor.getDvRuc() != null ? emisor.getRucBase() + "-" + emisor.getDvRuc() : emisor.getRucBase();
    }
}
