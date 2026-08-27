package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.catalogo.Producto;
import com.tottalstore.sifen.catalogo.ProductoRepository;
import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.clientes.ClienteRepository;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.facturacion.dto.FacturaRequest;
import com.tottalstore.sifen.facturacion.dto.ItemFacturaRequest;
import com.tottalstore.sifen.firma.DteFirmado;
import com.tottalstore.sifen.firma.FirmaDigitalService;
import com.tottalstore.sifen.shared.TasaIva;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.EnviadorSifenConfig;
import com.tottalstore.sifen.sifen.EnviadorSifenConfigRepository;
import com.tottalstore.sifen.sifen.EnviadorSifenService;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import com.tottalstore.sifen.sifen.RespuestaSifenRepository;
import com.tottalstore.sifen.sifen.RespuestaSifenResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Emisión manual de facturas: borrador, cálculo de IVA/totales y confirmación (RF-11 a RF-17, CU-02). */
@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final CurrentUser currentUser;
    private final FirmaDigitalService firmaDigitalService;
    private final EnviadorSifenService enviadorSifenService;
    private final EnviadorSifenConfigRepository enviadorSifenConfigRepository;
    private final RespuestaSifenRepository respuestaSifenRepository;
    private final AuditoriaService auditoriaService;

    public FacturaService(
            FacturaRepository facturaRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            CurrentUser currentUser,
            FirmaDigitalService firmaDigitalService,
            EnviadorSifenService enviadorSifenService,
            EnviadorSifenConfigRepository enviadorSifenConfigRepository,
            RespuestaSifenRepository respuestaSifenRepository,
            AuditoriaService auditoriaService) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.currentUser = currentUser;
        this.firmaDigitalService = firmaDigitalService;
        this.enviadorSifenService = enviadorSifenService;
        this.enviadorSifenConfigRepository = enviadorSifenConfigRepository;
        this.respuestaSifenRepository = respuestaSifenRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public FacturaElectronica crearBorrador(FacturaRequest request) {
        Cliente cliente = obtenerClienteActivo(request.clienteRuc());
        validarCondicionPago(request);

        FacturaElectronica factura = new FacturaElectronica();
        factura.setCliente(cliente);
        factura.setUsuario(currentUser.obtener());
        aplicarCondicionPago(factura, request);
        factura.setEstadoDte(EstadoDte.BORRADOR);

        aplicarItems(factura, request.items());
        calcularTotales(factura);

        FacturaElectronica guardada = facturaRepository.save(factura);
        auditoriaService.registrar("CREAR_BORRADOR", factura.getUsuario(), guardada);
        return guardada;
    }

    @Transactional
    public FacturaElectronica editarBorrador(UUID id, FacturaRequest request) {
        FacturaElectronica factura = obtener(id);
        exigirEstado(factura, EstadoDte.BORRADOR, "editar");

        Cliente cliente = obtenerClienteActivo(request.clienteRuc());
        validarCondicionPago(request);

        factura.setCliente(cliente);
        aplicarCondicionPago(factura, request);
        factura.getItems().clear();
        aplicarItems(factura, request.items());
        calcularTotales(factura);

        FacturaElectronica guardada = facturaRepository.save(factura);
        auditoriaService.registrar("EDITAR_BORRADOR", currentUser.obtener(), guardada);
        return guardada;
    }

    @Transactional
    public FacturaElectronica confirmarEnvio(UUID id) {
        FacturaElectronica factura = obtener(id);
        exigirEstado(factura, EstadoDte.BORRADOR, "confirmar y enviar");

        // CU-05: validación de campos críticos antes del envío (flujo alternativo 3a de CU-01/CU-02).
        factura.setEstadoDte(EstadoDte.EN_VALIDACION);
        validarParaEnvio(factura);

        // CU-03: firma digital del DTE (stub, ver FirmaDigitalService).
        DteFirmado firmado = firmaDigitalService.firmar(construirXmlSimplificado(factura));
        factura.setEstadoDte(EstadoDte.FIRMADO);

        // CU-04: envío al SIFEN (stub, ver EnviadorSifenService).
        factura.setEstadoDte(EstadoDte.ENVIADO_SIFEN);
        RespuestaSifenResult resultado = enviadorSifenService.enviar(firmado.xmlFirmado(), ambienteActual());

        RespuestaSifen respuesta = new RespuestaSifen();
        respuesta.setFactura(factura);
        respuesta.setCodigo(resultado.codigo());
        respuesta.setDescripcion(resultado.descripcion());
        respuesta.setCdc(resultado.cdc());
        respuestaSifenRepository.save(respuesta);

        factura.setEstadoDte(resultado.aprobado() ? EstadoDte.APROBADO : EstadoDte.RECHAZADO);
        FacturaElectronica guardada = facturaRepository.save(factura);

        auditoriaService.registrar(
                resultado.aprobado() ? "APROBADO_SIFEN" : "RECHAZADO_SIFEN", currentUser.obtener(), guardada);
        return guardada;
    }

    public FacturaElectronica obtener(UUID id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Factura no encontrada: " + id));
    }

    public List<FacturaElectronica> listar(EstadoDte estado) {
        return estado != null
                ? facturaRepository.findByEstadoDteOrderByFechaEmisionDesc(estado)
                : facturaRepository.findAllByOrderByFechaEmisionDesc();
    }

    public List<FacturaElectronica> historialPorCliente(String ruc) {
        return facturaRepository.findByClienteRucOrderByFechaEmisionDesc(ruc);
    }

    private Cliente obtenerClienteActivo(String ruc) {
        Cliente cliente = clienteRepository.findById(ruc)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + ruc));
        if (!cliente.isActivo()) {
            throw new BusinessException("El cliente " + ruc + " está desactivado");
        }
        return cliente;
    }

    private void validarCondicionPago(FacturaRequest request) {
        if (request.condicionPago() == CondicionPago.CREDITO
                && (request.plazoDias() == null || request.cantidadCuotas() == null)) {
            throw new BusinessException("Para condición de pago a crédito debe indicar plazo y cantidad de cuotas");
        }
    }

    private void aplicarCondicionPago(FacturaElectronica factura, FacturaRequest request) {
        factura.setCondicionPago(request.condicionPago());
        boolean esCredito = request.condicionPago() == CondicionPago.CREDITO;
        factura.setPlazoDias(esCredito ? request.plazoDias() : null);
        factura.setCantidadCuotas(esCredito ? request.cantidadCuotas() : null);
    }

    private void exigirEstado(FacturaElectronica factura, EstadoDte esperado, String accion) {
        if (factura.getEstadoDte() != esperado) {
            throw new BusinessException(
                    "No se puede " + accion + " una factura en estado " + factura.getEstadoDte());
        }
    }

    private void validarParaEnvio(FacturaElectronica factura) {
        if (factura.getItems().isEmpty()) {
            throw new BusinessException("La factura no tiene ítems");
        }
        if (!factura.getCliente().isActivo()) {
            throw new BusinessException("El cliente está desactivado");
        }
    }

    private Ambiente ambienteActual() {
        return enviadorSifenConfigRepository.findAll().stream()
                .findFirst()
                .map(EnviadorSifenConfig::getAmbiente)
                .orElse(Ambiente.TEST);
    }

    private String construirXmlSimplificado(FacturaElectronica factura) {
        // TODO: reemplazar por el generador real del XML del DTE conforme al Manual Técnico del
        // SIFEN (RNF-05); esta representación mínima solo sirve para ejercitar el flujo de firma/envío.
        return "<DTE><idFactura>" + factura.getId() + "</idFactura>"
                + "<clienteRuc>" + factura.getCliente().getRuc() + "</clienteRuc>"
                + "<totalGeneral>" + factura.getTotalGeneral() + "</totalGeneral></DTE>";
    }

    private void aplicarItems(FacturaElectronica factura, List<ItemFacturaRequest> itemsRequest) {
        for (ItemFacturaRequest itemRequest : itemsRequest) {
            ItemFactura item = new ItemFactura();
            item.setFactura(factura);
            item.setCantidad(itemRequest.cantidad());
            item.setPrecioUnitario(itemRequest.precioUnitario());
            item.setTasaIva(itemRequest.tasaIva());
            item.setDescripcion(itemRequest.descripcion());

            if (itemRequest.productoCodigo() != null && !itemRequest.productoCodigo().isBlank()) {
                Producto producto = productoRepository.findById(itemRequest.productoCodigo())
                        .orElseThrow(() -> new NotFoundException(
                                "Producto no encontrado: " + itemRequest.productoCodigo()));
                item.setProducto(producto);
            }

            item.setSubtotal(calcularSubtotalItem(itemRequest.cantidad(), itemRequest.precioUnitario()));
            factura.getItems().add(item);
        }
    }

    /** Cantidad × precio unitario, redondeado a Guaraníes enteros. */
    static BigDecimal calcularSubtotalItem(int cantidad, BigDecimal precioUnitario) {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calcula total_iva5, total_iva10 y total_general. Convención SIFEN: el subtotal de cada ítem
     * ya incluye el IVA, por lo que este se extrae (no se suma) a partir de la tasa correspondiente.
     */
    static void calcularTotales(FacturaElectronica factura) {
        BigDecimal totalIva5 = BigDecimal.ZERO;
        BigDecimal totalIva10 = BigDecimal.ZERO;
        BigDecimal totalGeneral = BigDecimal.ZERO;

        for (ItemFactura item : factura.getItems()) {
            BigDecimal itemTotal = item.getSubtotal();
            totalGeneral = totalGeneral.add(itemTotal);

            if (item.getTasaIva() == TasaIva.EXENTA) {
                continue;
            }

            BigDecimal divisor = BigDecimal.ONE.add(item.getTasaIva().fraccion());
            BigDecimal base = itemTotal.divide(divisor, 0, RoundingMode.HALF_UP);
            BigDecimal iva = itemTotal.subtract(base);

            if (item.getTasaIva() == TasaIva.CINCO) {
                totalIva5 = totalIva5.add(iva);
            } else {
                totalIva10 = totalIva10.add(iva);
            }
        }

        factura.setTotalIva5(totalIva5);
        factura.setTotalIva10(totalIva10);
        factura.setTotalGeneral(totalGeneral);
    }
}
