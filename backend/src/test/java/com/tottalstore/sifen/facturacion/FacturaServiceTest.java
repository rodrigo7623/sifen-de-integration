package com.tottalstore.sifen.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.catalogo.ProductoRepository;
import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.clientes.ClienteRepository;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.email.EnviadorEmailService;
import com.tottalstore.sifen.establecimientos.EstablecimientoRepository;
import com.tottalstore.sifen.firma.DteFirmado;
import com.tottalstore.sifen.firma.FirmaDigitalService;
import com.tottalstore.sifen.shared.TasaIva;
import com.tottalstore.sifen.sifen.EnviadorSifenConfigRepository;
import com.tottalstore.sifen.sifen.EnviadorSifenService;
import com.tottalstore.sifen.sifen.RespuestaSifenRepository;
import com.tottalstore.sifen.sifen.RespuestaSifenResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private EstablecimientoRepository establecimientoRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private FirmaDigitalService firmaDigitalService;
    @Mock
    private EnviadorSifenService enviadorSifenService;
    @Mock
    private EnviadorSifenConfigRepository enviadorSifenConfigRepository;
    @Mock
    private RespuestaSifenRepository respuestaSifenRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private EnviadorEmailService enviadorEmailService;
    @Mock
    private FacturaPdfGenerator facturaPdfGenerator;

    private FacturaService facturaService;

    @BeforeEach
    void setUp() {
        facturaService = new FacturaService(
                facturaRepository, clienteRepository, productoRepository, establecimientoRepository, currentUser,
                firmaDigitalService, enviadorSifenService, enviadorSifenConfigRepository,
                respuestaSifenRepository, auditoriaService, enviadorEmailService, facturaPdfGenerator);
    }

    @Test
    void calculaIvaExtrayendoloDelSubtotalIvaIncluido() {
        // Réplica del ejemplo del prototipo P-03: 2 x 350.000 + 1 x 550.000, ambos gravados al 10%.
        FacturaElectronica factura = new FacturaElectronica();
        factura.getItems().add(item(2, "350000", TasaIva.DIEZ));
        factura.getItems().add(item(1, "550000", TasaIva.DIEZ));

        FacturaService.calcularTotales(factura);

        assertThat(factura.getTotalGeneral()).isEqualByComparingTo("1250000");
        assertThat(factura.getTotalIva10()).isEqualByComparingTo("113636");
        assertThat(factura.getTotalIva5()).isEqualByComparingTo("0");
    }

    @Test
    void noExtraeIvaDeItemsExentos() {
        FacturaElectronica factura = new FacturaElectronica();
        factura.getItems().add(item(3, "100000", TasaIva.EXENTA));

        FacturaService.calcularTotales(factura);

        assertThat(factura.getTotalGeneral()).isEqualByComparingTo("300000");
        assertThat(factura.getTotalIva5()).isEqualByComparingTo("0");
        assertThat(factura.getTotalIva10()).isEqualByComparingTo("0");
    }

    @Test
    void confirmarEnvioApruebaFacturaCuandoSifenAprueba() {
        UUID id = UUID.randomUUID();
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(id);
        factura.setEstadoDte(EstadoDte.BORRADOR);
        factura.setCliente(clienteActivo());
        factura.getItems().add(item(1, "100000", TasaIva.DIEZ));

        when(facturaRepository.findById(id)).thenReturn(java.util.Optional.of(factura));
        when(firmaDigitalService.firmar(any())).thenReturn(new DteFirmado("<xml/>", Instant.now()));
        when(enviadorSifenService.enviar(any(), any(), any()))
                .thenReturn(new RespuestaSifenResult(true, "0260", "Aprobado", "CDC-SIMULADO"));
        when(enviadorSifenConfigRepository.findAll()).thenReturn(List.of());
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.obtener()).thenReturn(mock(Usuario.class));

        FacturaElectronica resultado = facturaService.confirmarEnvio(id);

        assertThat(resultado.getEstadoDte()).isEqualTo(EstadoDte.APROBADO);
        assertThat(resultado.isEmailEnviado()).isTrue();
        verify(respuestaSifenRepository).save(any());
        verify(enviadorEmailService).enviar(any(), any(), any());
    }

    @Test
    void noEnviaCorreoCuandoSifenRechaza() {
        UUID id = UUID.randomUUID();
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(id);
        factura.setEstadoDte(EstadoDte.BORRADOR);
        factura.setCliente(clienteActivo());
        factura.getItems().add(item(1, "100000", TasaIva.DIEZ));

        when(facturaRepository.findById(id)).thenReturn(java.util.Optional.of(factura));
        when(firmaDigitalService.firmar(any())).thenReturn(new DteFirmado("<xml/>", Instant.now()));
        when(enviadorSifenService.enviar(any(), any(), any()))
                .thenReturn(new RespuestaSifenResult(false, "0160", "Rechazado (motivo de prueba)", null));
        when(enviadorSifenConfigRepository.findAll()).thenReturn(List.of());
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.obtener()).thenReturn(mock(Usuario.class));

        FacturaElectronica resultado = facturaService.confirmarEnvio(id);

        assertThat(resultado.getEstadoDte()).isEqualTo(EstadoDte.RECHAZADO);
        assertThat(resultado.isEmailEnviado()).isFalse();
        verify(enviadorEmailService, org.mockito.Mockito.never()).enviar(any(), any(), any());
    }

    @Test
    void reintentarEnvioTrasUnRechazoActualizaLaMismaRespuestaSifenEnVezDeDuplicarla() {
        // Regresión: reabrir -> editar -> confirmar de nuevo violaba la restricción unique de
        // respuesta_sifen.factura_id porque se creaba una fila nueva en vez de actualizar la existente.
        UUID id = UUID.randomUUID();
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(id);
        factura.setEstadoDte(EstadoDte.BORRADOR);
        factura.setCliente(clienteActivo());
        factura.getItems().add(item(1, "100000", TasaIva.DIEZ));

        com.tottalstore.sifen.sifen.RespuestaSifen respuestaPrevia = new com.tottalstore.sifen.sifen.RespuestaSifen();
        respuestaPrevia.setId(UUID.randomUUID());
        factura.setRespuestaSifen(respuestaPrevia);

        when(facturaRepository.findById(id)).thenReturn(java.util.Optional.of(factura));
        when(firmaDigitalService.firmar(any())).thenReturn(new DteFirmado("<xml/>", Instant.now()));
        when(enviadorSifenService.enviar(any(), any(), any()))
                .thenReturn(new RespuestaSifenResult(true, "0260", "Aprobado", "CDC-2"));
        when(enviadorSifenConfigRepository.findAll()).thenReturn(List.of());
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.obtener()).thenReturn(mock(Usuario.class));

        facturaService.confirmarEnvio(id);

        verify(respuestaSifenRepository).save(org.mockito.ArgumentMatchers.same(respuestaPrevia));
    }

    @Test
    void noPermiteConfirmarUnaFacturaQueNoEstaEnBorrador() {
        UUID id = UUID.randomUUID();
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(id);
        factura.setEstadoDte(EstadoDte.APROBADO);
        when(facturaRepository.findById(id)).thenReturn(java.util.Optional.of(factura));

        assertThatThrownBy(() -> facturaService.confirmarEnvio(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reabrirVuelveUnaFacturaRechazadaABorrador() {
        UUID id = UUID.randomUUID();
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(id);
        factura.setEstadoDte(EstadoDte.RECHAZADO);
        when(facturaRepository.findById(id)).thenReturn(java.util.Optional.of(factura));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.obtener()).thenReturn(mock(Usuario.class));

        FacturaElectronica resultado = facturaService.reabrir(id);

        assertThat(resultado.getEstadoDte()).isEqualTo(EstadoDte.BORRADOR);
        verify(auditoriaService).registrar(org.mockito.ArgumentMatchers.eq("REABRIR_RECHAZADO"), any(), any());
    }

    @Test
    void noPermiteReabrirUnaFacturaQueNoEstaRechazada() {
        UUID id = UUID.randomUUID();
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(id);
        factura.setEstadoDte(EstadoDte.BORRADOR);
        when(facturaRepository.findById(id)).thenReturn(java.util.Optional.of(factura));

        assertThatThrownBy(() -> facturaService.reabrir(id))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void crearBorradorAsignaElEstablecimientoYPuntoDeExpedicionElegidos() {
        com.tottalstore.sifen.establecimientos.Establecimiento establecimiento = establecimientoConPunto();
        com.tottalstore.sifen.establecimientos.PuntoExpedicion punto = establecimiento.getPuntosExpedicion().get(0);

        when(clienteRepository.findById("80012345-0")).thenReturn(java.util.Optional.of(clienteActivo()));
        when(establecimientoRepository.findById(establecimiento.getId()))
                .thenReturn(java.util.Optional.of(establecimiento));
        when(facturaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.obtener()).thenReturn(mock(Usuario.class));

        var request = new com.tottalstore.sifen.facturacion.dto.FacturaRequest(
                "80012345-0",
                establecimiento.getId(),
                punto.getId(),
                CondicionPago.CONTADO,
                null,
                null,
                List.of(new com.tottalstore.sifen.facturacion.dto.ItemFacturaRequest(
                        null, "Ítem de prueba", 1, new BigDecimal("10000"), TasaIva.DIEZ)));

        FacturaElectronica creada = facturaService.crearBorrador(request);

        assertThat(creada.getEstablecimiento()).isSameAs(establecimiento);
        assertThat(creada.getPuntoExpedicion()).isSameAs(punto);
    }

    @Test
    void noPermiteCrearUnaFacturaConUnPuntoDeExpedicionDesactivado() {
        com.tottalstore.sifen.establecimientos.Establecimiento establecimiento = establecimientoConPunto();
        com.tottalstore.sifen.establecimientos.PuntoExpedicion punto = establecimiento.getPuntosExpedicion().get(0);
        punto.setActivo(false);

        when(clienteRepository.findById("80012345-0")).thenReturn(java.util.Optional.of(clienteActivo()));
        when(establecimientoRepository.findById(establecimiento.getId()))
                .thenReturn(java.util.Optional.of(establecimiento));

        var request = new com.tottalstore.sifen.facturacion.dto.FacturaRequest(
                "80012345-0",
                establecimiento.getId(),
                punto.getId(),
                CondicionPago.CONTADO,
                null,
                null,
                List.of(new com.tottalstore.sifen.facturacion.dto.ItemFacturaRequest(
                        null, "Ítem de prueba", 1, new BigDecimal("10000"), TasaIva.DIEZ)));

        assertThatThrownBy(() -> facturaService.crearBorrador(request))
                .isInstanceOf(BusinessException.class);
    }

    private com.tottalstore.sifen.establecimientos.Establecimiento establecimientoConPunto() {
        com.tottalstore.sifen.establecimientos.Establecimiento establecimiento =
                new com.tottalstore.sifen.establecimientos.Establecimiento();
        establecimiento.setId(UUID.randomUUID());
        establecimiento.setCodigo("001");
        establecimiento.setDenominacion("Casa matriz");

        com.tottalstore.sifen.establecimientos.PuntoExpedicion punto =
                new com.tottalstore.sifen.establecimientos.PuntoExpedicion();
        punto.setId(UUID.randomUUID());
        punto.setEstablecimiento(establecimiento);
        punto.setCodigo("001");
        establecimiento.getPuntosExpedicion().add(punto);
        return establecimiento;
    }

    private ItemFactura item(int cantidad, String precioUnitario, TasaIva tasaIva) {
        ItemFactura item = new ItemFactura();
        item.setCantidad(cantidad);
        item.setPrecioUnitario(new BigDecimal(precioUnitario));
        item.setTasaIva(tasaIva);
        item.setSubtotal(FacturaService.calcularSubtotalItem(cantidad, new BigDecimal(precioUnitario)));
        return item;
    }

    private Cliente clienteActivo() {
        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-0");
        cliente.setActivo(true);
        return cliente;
    }
}
