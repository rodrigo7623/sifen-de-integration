package com.tottalstore.sifen.registro;

import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.registro.dto.ComprobanteRegistradoRequest;
import com.tottalstore.sifen.shared.Modulo11;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga de los comprobantes que no emite el SIFEN de Tottal Store: compras (Libro IVA Compras y
 * crédito fiscal) y ventas con comprobantes preimpresos o tickets. Aplica las reglas de la
 * Especificación Técnica del Registro de Comprobantes (RG 90/2021) para que el archivo que se exporta
 * no lo rechace Marangatu.
 */
@Service
public class ComprobanteRegistradoService {

    /** La RG 90 no admite comprobantes anteriores a esta fecha, salvo los de condición crédito. */
    static final LocalDate FECHA_MINIMA = LocalDate.of(2021, 1, 1);

    private static final BigDecimal ONCE = BigDecimal.valueOf(11);
    private static final BigDecimal VEINTIUNO = BigDecimal.valueOf(21);

    private final ComprobanteRegistradoRepository repository;
    private final CurrentUser currentUser;

    public ComprobanteRegistradoService(ComprobanteRegistradoRepository repository, CurrentUser currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public List<ComprobanteRegistrado> listar(YearMonth periodo, TipoRegistro tipo) {
        LocalDate desde = periodo.atDay(1);
        LocalDate hasta = periodo.plusMonths(1).atDay(1);
        return tipo == null ? repository.delPeriodo(desde, hasta) : repository.delPeriodo(tipo, desde, hasta);
    }

    public ComprobanteRegistrado obtener(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Comprobante no encontrado"));
    }

    @Transactional
    public ComprobanteRegistrado crear(ComprobanteRegistradoRequest request) {
        ComprobanteRegistrado c = new ComprobanteRegistrado();
        aplicar(c, request);
        if (c.getNumero() != null && repository.existsByTipoRegistroAndTipoComprobanteAndIdentificacionAndTimbradoAndNumero(
                c.getTipoRegistro(), c.getTipoComprobante(), c.getIdentificacion(), c.getTimbrado(), c.getNumero())) {
            throw duplicado(c);
        }
        c.setCreadoPor(currentUser.obtener());
        c.setModificadoPor(c.getCreadoPor());
        return repository.save(c);
    }

    @Transactional
    public ComprobanteRegistrado editar(UUID id, ComprobanteRegistradoRequest request) {
        ComprobanteRegistrado c = obtener(id);
        aplicar(c, request);
        if (c.getNumero() != null
                && repository.existsByTipoRegistroAndTipoComprobanteAndIdentificacionAndTimbradoAndNumeroAndIdNot(
                        c.getTipoRegistro(), c.getTipoComprobante(), c.getIdentificacion(), c.getTimbrado(),
                        c.getNumero(), id)) {
            throw duplicado(c);
        }
        c.setModificadoPor(currentUser.obtener());
        c.setUpdatedAt(Instant.now());
        return repository.save(c);
    }

    @Transactional
    public void eliminar(UUID id) {
        repository.delete(obtener(id));
    }

    private BusinessException duplicado(ComprobanteRegistrado c) {
        return new BusinessException("Ya está registrado: " + c.getTipoComprobante().descripcion() + " " + c.getNumero()
                + " de " + c.getIdentificacion() + " con timbrado " + c.getTimbrado());
    }

    /** Valida todas las reglas a la vez (para que el usuario vea todos los errores juntos) y copia los datos. */
    void aplicar(ComprobanteRegistrado c, ComprobanteRegistradoRequest r) {
        List<String> errores = new ArrayList<>();
        TipoRegistro registro = r.tipoRegistro();
        TipoComprobanteRg90 tipo = r.tipoComprobante();
        boolean compra = registro == TipoRegistro.COMPRA;

        if (!tipo.admite(registro)) {
            errores.add(tipo.descripcion() + " no se puede registrar como " + (compra ? "compra" : "venta"));
        }

        CondicionPago condicion = r.condicion() != null ? r.condicion() : CondicionPago.CONTADO;
        if (r.fechaEmision().isBefore(FECHA_MINIMA) && condicion != CondicionPago.CREDITO) {
            errores.add("La RG 90 no admite comprobantes de contado anteriores al 01/01/2021");
        }
        if (r.fechaEmision().isAfter(LocalDate.now(java.time.ZoneId.of("America/Asuncion")))) {
            errores.add("La fecha de emisión no puede ser futura");
        }

        String identificacion = r.identificacion().trim().toUpperCase();
        if (compra && tipo.exigeRucEnCompras() && r.tipoIdentificacion() != TipoIdentificacion.RUC) {
            errores.add("En compras el proveedor se identifica con su RUC (salvo autofactura y despacho de importación)");
        }
        if (r.tipoIdentificacion() == TipoIdentificacion.RUC) {
            String error = validarRuc(identificacion);
            if (error != null) {
                errores.add(error);
            }
        }
        String razonSocial = blankANull(r.razonSocial());
        if (razonSocial == null && !r.tipoIdentificacion().nombreOpcional()) {
            errores.add("La razón social es obligatoria para " + r.tipoIdentificacion().descripcion().toLowerCase());
        }

        String timbrado = blankANull(r.timbrado());
        if (tipo.sinTimbrado()) {
            timbrado = "0";
        } else if (timbrado == null || timbrado.length() != 8) {
            errores.add("El timbrado es obligatorio y tiene 8 dígitos");
        }

        String numero = blankANull(r.numero());
        if (numero == null && !tipo.numeroOpcional()) {
            errores.add("El número de comprobante es obligatorio (formato 001-001-0000001)");
        }

        String cdc = blankANull(r.cdc());
        if (cdc != null && !compra) {
            errores.add("Las ventas electrónicas se toman del SIFEN: acá solo van ventas con comprobantes preimpresos o tickets");
        }

        BigDecimal g10 = cero(r.gravado10());
        BigDecimal g5 = cero(r.gravado5());
        BigDecimal ex = cero(r.exento());
        BigDecimal total = r.total();
        if (tipo.soloTotal(registro)) {
            // Autofactura, boleta resimple, loterías y ticket: en compras solo se informa el total.
            g10 = BigDecimal.ZERO;
            g5 = BigDecimal.ZERO;
            ex = BigDecimal.ZERO;
        } else if (total.compareTo(g10.add(g5).add(ex)) != 0) {
            errores.add("El total (" + total.toPlainString() + ") tiene que ser la suma del gravado 10%, el gravado 5% y el exento ("
                    + g10.add(g5).add(ex).toPlainString() + ")");
        }
        if (total.signum() <= 0) {
            errores.add("El total tiene que ser mayor a cero");
        }

        if (!r.imputaIva() && !r.imputaIre() && !r.imputaIrp()) {
            errores.add("El comprobante tiene que imputarse al menos al IVA, al IRE o al IRP-RSP");
        }

        String asociadoNumero = blankANull(r.asociadoNumero());
        String asociadoTimbrado = blankANull(r.asociadoTimbrado());
        if (tipo.esNota()) {
            if (asociadoNumero == null || asociadoTimbrado == null || asociadoTimbrado.length() != 8) {
                errores.add("La nota de crédito o débito necesita el número y el timbrado (8 dígitos) del comprobante que ajusta");
            }
        } else {
            asociadoNumero = null;
            asociadoTimbrado = null;
        }

        if (!errores.isEmpty()) {
            throw new BusinessException(String.join(" · ", errores));
        }

        c.setTipoRegistro(registro);
        c.setTipoComprobante(tipo);
        c.setFechaEmision(r.fechaEmision());
        c.setTipoIdentificacion(r.tipoIdentificacion());
        c.setIdentificacion(identificacion);
        c.setRazonSocial(razonSocial);
        c.setTimbrado(timbrado);
        c.setNumero(numero);
        c.setCdc(cdc);
        c.setCondicion(condicion);
        c.setMonedaExtranjera(r.monedaExtranjera());
        c.setGravado10(g10);
        c.setGravado5(g5);
        c.setExento(ex);
        c.setTotal(total);
        c.setIva10(ivaIncluido(g10, ONCE));
        c.setIva5(ivaIncluido(g5, VEINTIUNO));
        c.setImputaIva(r.imputaIva());
        c.setImputaIre(r.imputaIre());
        c.setImputaIrp(r.imputaIrp());
        c.setNoImputa(r.noImputa());
        c.setAsociadoNumero(asociadoNumero);
        c.setAsociadoTimbrado(asociadoTimbrado);
        c.setObservacion(blankANull(r.observacion()));
    }

    /** IVA contenido en un monto con IVA incluido: 10% → monto/11, 5% → monto/21 (redondeado al guaraní). */
    static BigDecimal ivaIncluido(BigDecimal montoConIva, BigDecimal divisor) {
        return montoConIva.divide(divisor, 0, RoundingMode.HALF_UP);
    }

    /** RUC con DV ("80012345-6"): el DV tiene que coincidir con el módulo 11. Sin DV también se acepta. */
    static String validarRuc(String ruc) {
        if (!ruc.matches("[0-9A-Z]{1,8}(-\\d)?")) {
            return "El RUC va con el formato 80012345-6";
        }
        int guion = ruc.indexOf('-');
        if (guion > 0) {
            int dv = Modulo11.calcularDigitoVerificador(ruc.substring(0, guion));
            if (dv != Character.getNumericValue(ruc.charAt(guion + 1))) {
                return "El dígito verificador del RUC " + ruc + " no es correcto (debería ser " + dv + ")";
            }
        }
        return null;
    }

    private static BigDecimal cero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private static String blankANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
