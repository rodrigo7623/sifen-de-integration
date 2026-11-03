package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.catalogo.Producto;
import com.tottalstore.sifen.catalogo.ProductoRepository;
import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.clientes.ClienteRepository;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.email.EnviadorEmailService;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.EstablecimientoRepository;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.establecimientos.PuntoExpedicionRepository;
import com.tottalstore.sifen.facturacion.dto.FacturaRequest;
import com.tottalstore.sifen.facturacion.dto.ItemFacturaRequest;
import com.tottalstore.sifen.facturacion.dto.NotaRequest;
import com.tottalstore.sifen.firma.DteFirmado;
import com.tottalstore.sifen.firma.GeneradorDteService;
import com.tottalstore.sifen.shared.TasaIva;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.EnviadorSifenConfig;
import com.tottalstore.sifen.sifen.EnviadorSifenConfigRepository;
import com.tottalstore.sifen.sifen.EnviadorSifenService;
import com.tottalstore.sifen.sifen.IntentoEnvioSifen;
import com.tottalstore.sifen.sifen.IntentoEnvioSifenRepository;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import com.tottalstore.sifen.sifen.RespuestaSifenRepository;
import com.tottalstore.sifen.sifen.RespuestaSifenResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private final EstablecimientoRepository establecimientoRepository;
    private final PuntoExpedicionRepository puntoExpedicionRepository;
    private final CurrentUser currentUser;
    private final GeneradorDteService generadorDteService;
    private final EnviadorSifenService enviadorSifenService;
    private final EnviadorSifenConfigRepository enviadorSifenConfigRepository;
    private final RespuestaSifenRepository respuestaSifenRepository;
    private final IntentoEnvioSifenRepository intentoEnvioSifenRepository;
    private final AuditoriaService auditoriaService;
    private final EnviadorEmailService enviadorEmailService;
    private final FacturaPdfGenerator facturaPdfGenerator;

    public FacturaService(
            FacturaRepository facturaRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            EstablecimientoRepository establecimientoRepository,
            PuntoExpedicionRepository puntoExpedicionRepository,
            CurrentUser currentUser,
            GeneradorDteService generadorDteService,
            EnviadorSifenService enviadorSifenService,
            EnviadorSifenConfigRepository enviadorSifenConfigRepository,
            RespuestaSifenRepository respuestaSifenRepository,
            IntentoEnvioSifenRepository intentoEnvioSifenRepository,
            AuditoriaService auditoriaService,
            EnviadorEmailService enviadorEmailService,
            FacturaPdfGenerator facturaPdfGenerator) {
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.establecimientoRepository = establecimientoRepository;
        this.puntoExpedicionRepository = puntoExpedicionRepository;
        this.currentUser = currentUser;
        this.generadorDteService = generadorDteService;
        this.enviadorSifenService = enviadorSifenService;
        this.enviadorSifenConfigRepository = enviadorSifenConfigRepository;
        this.respuestaSifenRepository = respuestaSifenRepository;
        this.intentoEnvioSifenRepository = intentoEnvioSifenRepository;
        this.auditoriaService = auditoriaService;
        this.enviadorEmailService = enviadorEmailService;
        this.facturaPdfGenerator = facturaPdfGenerator;
    }

    @Transactional
    public FacturaElectronica crearBorrador(FacturaRequest request) {
        Cliente cliente = obtenerClienteActivo(request.clienteRuc());
        PuntoExpedicion puntoExpedicion = resolverPuntoExpedicion(request.establecimientoId(), request.puntoExpedicionId());
        validarCondicionPago(request);

        FacturaElectronica factura = new FacturaElectronica();
        factura.setCliente(cliente);
        factura.setEstablecimiento(puntoExpedicion.getEstablecimiento());
        factura.setPuntoExpedicion(puntoExpedicion);
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
        if (factura.esNota()) {
            throw new BusinessException("Las notas de crédito/débito se editan desde /api/notas");
        }
        exigirEstado(factura, EstadoDte.BORRADOR, "editar");

        Cliente cliente = obtenerClienteActivo(request.clienteRuc());
        PuntoExpedicion puntoExpedicion = resolverPuntoExpedicion(request.establecimientoId(), request.puntoExpedicionId());
        validarCondicionPago(request);

        factura.setCliente(cliente);
        factura.setEstablecimiento(puntoExpedicion.getEstablecimiento());
        factura.setPuntoExpedicion(puntoExpedicion);
        aplicarCondicionPago(factura, request);
        factura.getItems().clear();
        aplicarItems(factura, request.items());
        calcularTotales(factura);

        FacturaElectronica guardada = facturaRepository.save(factura);
        auditoriaService.registrar("EDITAR_BORRADOR", currentUser.obtener(), guardada);
        return guardada;
    }

    /**
     * M6: borrador de nota de crédito/débito sobre una factura aprobada. Cliente, establecimiento y
     * punto de expedición son los de la factura; la nota se confirma y envía como cualquier DE.
     */
    @Transactional
    public FacturaElectronica crearNota(NotaRequest request) {
        if (!request.tipo().esNota()) {
            throw new BusinessException("El tipo de nota debe ser NC (crédito) o ND (débito)");
        }
        FacturaElectronica asociada = obtener(request.facturaAsociadaId());
        validarFacturaAsociada(asociada);

        FacturaElectronica nota = new FacturaElectronica();
        nota.setTipoDoc(request.tipo());
        nota.setFacturaAsociada(asociada);
        nota.setMotivoEmision(request.motivo());
        nota.setCliente(asociada.getCliente());
        nota.setEstablecimiento(asociada.getEstablecimiento());
        nota.setPuntoExpedicion(asociada.getPuntoExpedicion());
        nota.setUsuario(currentUser.obtener());
        nota.setEstadoDte(EstadoDte.BORRADOR);

        aplicarItems(nota, request.items());
        calcularTotales(nota);
        validarSaldoAcreditable(nota);

        FacturaElectronica guardada = facturaRepository.save(nota);
        auditoriaService.registrar("CREAR_NOTA_" + request.tipo(), nota.getUsuario(), guardada);
        return guardada;
    }

    @Transactional
    public FacturaElectronica editarNota(UUID id, NotaRequest request) {
        FacturaElectronica nota = obtener(id);
        if (!nota.esNota()) {
            throw new BusinessException("El documento " + id + " no es una nota de crédito/débito");
        }
        exigirEstado(nota, EstadoDte.BORRADOR, "editar");
        if (request.tipo() != nota.getTipoDoc() || !request.facturaAsociadaId().equals(nota.getFacturaAsociada().getId())) {
            throw new BusinessException("No se puede cambiar el tipo ni la factura asociada de una nota");
        }

        nota.setMotivoEmision(request.motivo());
        nota.getItems().clear();
        aplicarItems(nota, request.items());
        calcularTotales(nota);
        validarSaldoAcreditable(nota);

        FacturaElectronica guardada = facturaRepository.save(nota);
        auditoriaService.registrar("EDITAR_NOTA", currentUser.obtener(), guardada);
        return guardada;
    }

    /** Notas de crédito/débito emitidas sobre una factura, en orden de emisión. */
    public List<FacturaElectronica> notasDe(UUID facturaId) {
        obtener(facturaId);
        return facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(facturaId);
    }

    /**
     * H004 dCdCDERef: solo se puede ajustar una factura electrónica aprobada (tiene CDC en el SIFEN).
     * Una factura ANULADA ya fue acreditada en su totalidad.
     */
    private void validarFacturaAsociada(FacturaElectronica asociada) {
        if (asociada.getTipoDoc() != TipoDocumento.FE) {
            throw new BusinessException("Una nota solo puede asociarse a una factura electrónica");
        }
        if (asociada.getEstadoDte() != EstadoDte.APROBADO || asociada.cdcVigente() == null) {
            throw new BusinessException("Solo se pueden emitir notas sobre facturas aprobadas por el SIFEN (estado actual: "
                    + asociada.getEstadoDte() + ")");
        }
    }

    /**
     * Rechazo 2417 del SIFEN: la suma de las notas de crédito no puede superar el total de la factura.
     * Se cuentan también las notas pendientes de envío para no emitir dos que juntas lo excedan; las
     * rechazadas no cuentan (no existen para el SIFEN).
     */
    private void validarSaldoAcreditable(FacturaElectronica nota) {
        if (nota.getTipoDoc() != TipoDocumento.NC) {
            return;
        }
        BigDecimal saldo = saldoAcreditable(nota.getFacturaAsociada(), nota.getId());
        if (nota.getTotalGeneral().compareTo(saldo) > 0) {
            throw new BusinessException("El total de la nota de crédito (" + nota.getTotalGeneral().stripTrailingZeros().toPlainString()
                    + ") supera el saldo acreditable de la factura (" + saldo.stripTrailingZeros().toPlainString() + ")");
        }
    }

    /** Total de la factura menos las notas de crédito que no fueron rechazadas, sin contar {@code excluir}. */
    BigDecimal saldoAcreditable(FacturaElectronica factura, UUID excluir) {
        BigDecimal acreditado = facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(factura.getId()).stream()
                .filter(n -> n.getTipoDoc() == TipoDocumento.NC)
                .filter(n -> n.getEstadoDte() != EstadoDte.RECHAZADO)
                .filter(n -> !n.getId().equals(excluir))
                .map(FacturaElectronica::getTotalGeneral)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return factura.getTotalGeneral().subtract(acreditado);
    }

    /** M6 "anulación mediante nota de crédito": la factura acreditada por completo pasa a ANULADO. */
    private void anularSiQuedoAcreditada(FacturaElectronica nota) {
        FacturaElectronica factura = nota.getFacturaAsociada();
        BigDecimal aprobado = facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(factura.getId()).stream()
                .filter(n -> n.getTipoDoc() == TipoDocumento.NC)
                .filter(n -> n.getEstadoDte() == EstadoDte.APROBADO && !n.getId().equals(nota.getId()))
                .map(FacturaElectronica::getTotalGeneral)
                .reduce(nota.getTotalGeneral(), BigDecimal::add);
        if (factura.getEstadoDte() == EstadoDte.APROBADO && aprobado.compareTo(factura.getTotalGeneral()) >= 0) {
            factura.setEstadoDte(EstadoDte.ANULADO);
            facturaRepository.save(factura);
            auditoriaService.registrar("ANULADO_POR_NC", currentUser.obtener(), factura);
        }
    }

    @Transactional
    public FacturaElectronica confirmarEnvio(UUID id) {
        FacturaElectronica factura = obtener(id);
        exigirEstado(factura, EstadoDte.BORRADOR, "confirmar y enviar");

        // CU-05: validación de campos críticos antes del envío (flujo alternativo 3a de CU-01/CU-02).
        factura.setEstadoDte(EstadoDte.EN_VALIDACION);
        validarParaEnvio(factura);

        // La fecha de emisión del DE es la de la confirmación, no la de creación del borrador: el
        // SIFEN acepta como máximo 30 días de atraso (D002).
        asignarNumeroDocumento(factura);
        factura.setFechaEmision(Instant.now());

        // CU-03: generación del XML del DE y firma digital (ver GeneradorDteService).
        Ambiente ambiente = ambienteActual();
        DteFirmado firmado = generadorDteService.generar(factura, ambiente);
        factura.setCdc(firmado.cdc());
        factura.setCodigoSeguridad(firmado.codigoSeguridad());
        factura.setEstadoDte(EstadoDte.FIRMADO);

        // CU-04: envío al SIFEN (ver EnviadorSifenService).
        factura.setEstadoDte(EstadoDte.ENVIADO_SIFEN);
        RespuestaSifenResult resultado = enviadorSifenService.enviar(factura.getId(), firmado.xmlFirmado(), ambiente);

        // Si esta factura ya fue rechazada antes (reabrir → editar → confirmar de nuevo), ya existe
        // una fila en respuesta_sifen (factura_id es unique): hay que actualizarla, no insertar otra.
        RespuestaSifen respuesta = factura.getRespuestaSifen() != null ? factura.getRespuestaSifen() : new RespuestaSifen();
        respuesta.setFactura(factura);
        respuesta.setCodigo(resultado.codigo());
        respuesta.setDescripcion(resultado.descripcion());
        respuesta.setCdc(resultado.cdc());
        respuesta.setXmlFirmado(firmado.xmlFirmado());
        respuestaSifenRepository.save(respuesta);
        // Lado inverso del OneToOne: sin esto, la factura devuelta en esta misma request todavía no
        // "ve" la respuesta que se acaba de guardar (JPA no sincroniza el lado mappedBy en memoria).
        factura.setRespuestaSifen(respuesta);

        // Historial de intentos para las estadísticas de rechazo (respuesta_sifen solo guarda el último).
        IntentoEnvioSifen intento = new IntentoEnvioSifen();
        intento.setFactura(factura);
        intento.setAmbiente(ambiente);
        intento.setAprobado(resultado.aprobado());
        intento.setCodigo(resultado.codigo());
        intento.setDescripcion(resultado.descripcion());
        intentoEnvioSifenRepository.save(intento);

        factura.setEstadoDte(resultado.aprobado() ? EstadoDte.APROBADO : EstadoDte.RECHAZADO);
        FacturaElectronica guardada = facturaRepository.save(factura);

        auditoriaService.registrar(
                resultado.aprobado() ? "APROBADO_SIFEN" : "RECHAZADO_SIFEN", currentUser.obtener(), guardada);

        if (resultado.aprobado()) {
            if (guardada.getTipoDoc() == TipoDocumento.NC) {
                anularSiQuedoAcreditada(guardada);
            }
            enviarPorCorreo(guardada);
        }
        return guardada;
    }

    /** RF-09: envío del PDF+XML de la factura aprobada al correo del cliente (stub, ver EnviadorEmailService). */
    private void enviarPorCorreo(FacturaElectronica factura) {
        byte[] pdf = facturaPdfGenerator.generar(factura);
        String xml = factura.getRespuestaSifen().getXmlFirmado();
        enviadorEmailService.enviar(factura, pdf, xml);
        factura.setEmailEnviado(true);
        facturaRepository.save(factura);
        auditoriaService.registrar("EMAIL_ENVIADO", currentUser.obtener(), factura);
    }

    /**
     * RF-07: reabre una factura RECHAZADA para que el usuario corrija ítems/cliente y vuelva a
     * confirmar (reutiliza editarBorrador/confirmarEnvio sin cambios).
     */
    @Transactional
    public FacturaElectronica reabrir(UUID id) {
        FacturaElectronica factura = obtener(id);
        exigirEstado(factura, EstadoDte.RECHAZADO, "reabrir");

        factura.setEstadoDte(EstadoDte.BORRADOR);
        FacturaElectronica guardada = facturaRepository.save(factura);
        auditoriaService.registrar("REABRIR_RECHAZADO", currentUser.obtener(), guardada);
        return guardada;
    }

    public FacturaElectronica obtener(UUID id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Factura no encontrada: " + id));
    }

    /** RF-10: XML tal como se firmó y envió al SIFEN. Solo existe una vez que la factura fue enviada. */
    public String obtenerXmlFirmado(UUID id) {
        FacturaElectronica factura = obtener(id);
        if (factura.getRespuestaSifen() == null) {
            throw new NotFoundException("La factura " + id + " todavía no fue enviada al SIFEN");
        }
        return factura.getRespuestaSifen().getXmlFirmado();
    }

    public List<FacturaElectronica> listar(EstadoDte estado, TipoDocumento tipo) {
        List<FacturaElectronica> documentos = estado != null
                ? facturaRepository.findByEstadoDteOrderByFechaEmisionDesc(estado)
                : facturaRepository.findAllByOrderByFechaEmisionDesc();
        return tipo != null ? documentos.stream().filter(f -> f.getTipoDoc() == tipo).toList() : documentos;
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

    /** Resuelve y valida el establecimiento/punto de expedición elegidos al cargar la factura --
     * soporte de múltiples establecimientos, cada uno con sus propios puntos de expedición. */
    private PuntoExpedicion resolverPuntoExpedicion(UUID establecimientoId, UUID puntoExpedicionId) {
        Establecimiento establecimiento = establecimientoRepository.findById(establecimientoId)
                .orElseThrow(() -> new NotFoundException("Establecimiento no encontrado: " + establecimientoId));
        if (!establecimiento.isActivo()) {
            throw new BusinessException("El establecimiento " + establecimiento.getCodigo() + " está desactivado");
        }
        PuntoExpedicion puntoExpedicion = establecimiento.getPuntosExpedicion().stream()
                .filter(p -> p.getId().equals(puntoExpedicionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "El punto de expedición " + puntoExpedicionId
                                + " no pertenece al establecimiento " + establecimiento.getCodigo()));
        if (!puntoExpedicion.isActivo()) {
            throw new BusinessException(
                    "El punto de expedición " + puntoExpedicion.getCodigo() + " está desactivado");
        }
        return puntoExpedicion;
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
        if (factura.esNota()) {
            // La factura pudo haber cambiado desde que se creó el borrador (p. ej. otra nota la anuló).
            validarFacturaAsociada(factura.getFacturaAsociada());
            validarSaldoAcreditable(factura);
        }
    }

    private Ambiente ambienteActual() {
        return enviadorSifenConfigRepository.findAll().stream()
                .findFirst()
                .map(EnviadorSifenConfig::getAmbiente)
                .orElse(Ambiente.TEST);
    }

    /**
     * C007 dNumDoc: el siguiente número del punto de expedición para ese tipo de documento (facturas,
     * notas de crédito y notas de débito se numeran por separado), con la fila bloqueada para que dos
     * confirmaciones simultáneas no reciban el mismo. Un DE rechazado que se reabre y se reenvía
     * conserva su número (un DE rechazado no consume numeración en el SIFEN).
     */
    private void asignarNumeroDocumento(FacturaElectronica factura) {
        if (factura.getNumeroDocumento() != null) {
            return;
        }
        PuntoExpedicion punto = puntoExpedicionRepository.findByIdParaNumerar(factura.getPuntoExpedicion().getId())
                .orElseThrow(() -> new NotFoundException("Punto de expedición no encontrado"));
        int siguiente;
        switch (factura.getTipoDoc()) {
            case NC -> {
                siguiente = punto.getUltimoNumeroNc() + 1;
                punto.setUltimoNumeroNc(siguiente);
            }
            case ND -> {
                siguiente = punto.getUltimoNumeroNd() + 1;
                punto.setUltimoNumeroNd(siguiente);
            }
            default -> {
                siguiente = punto.getUltimoNumeroDocumento() + 1;
                punto.setUltimoNumeroDocumento(siguiente);
            }
        }
        puntoExpedicionRepository.save(punto);
        factura.setNumeroDocumento(siguiente);
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
