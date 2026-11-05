package com.tottalstore.sifen.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.registro.dto.ComprobanteRegistradoRequest;
import com.tottalstore.sifen.shared.Modulo11;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComprobanteRegistradoServiceTest {

    static final String RUC_PROVEEDOR = "80012345-" + Modulo11.calcularDigitoVerificador("80012345");

    @Mock
    private ComprobanteRegistradoRepository repository;
    @Mock
    private CurrentUser currentUser;

    private ComprobanteRegistradoService service;

    @BeforeEach
    void setUp() {
        service = new ComprobanteRegistradoService(repository, currentUser);
    }

    /** Factura de compra contado de 1.100.000 al 10% + 210.000 al 5% + 50.000 exento. */
    static ComprobanteRegistradoRequest compra(TipoComprobanteRg90 tipo, String numero, String total) {
        return new ComprobanteRegistradoRequest(TipoRegistro.COMPRA, tipo, LocalDate.of(2026, 11, 3),
                TipoIdentificacion.RUC, RUC_PROVEEDOR, "PROVEEDOR S.A.", "12345678", numero, null,
                CondicionPago.CONTADO, false, new BigDecimal("1100000"), new BigDecimal("210000"),
                new BigDecimal("50000"), new BigDecimal(total), true, true, false, false, null, null, null);
    }

    @Test
    void calculaElIvaIncluidoYGuardaQuienLoCargo() {
        Usuario usuario = new Usuario();
        usuario.setEmail("operario@tottalstore.com");
        when(currentUser.obtener()).thenReturn(usuario);
        when(repository.save(any(ComprobanteRegistrado.class))).thenAnswer(inv -> inv.getArgument(0));

        ComprobanteRegistrado c = service.crear(compra(TipoComprobanteRg90.FACTURA, "001-001-0000123", "1360000"));

        assertThat(c.getIva10()).isEqualByComparingTo("100000");
        assertThat(c.getIva5()).isEqualByComparingTo("10000");
        assertThat(c.getCreadoPor()).isSameAs(usuario);
        assertThat(c.identificacionSinDv()).isEqualTo("80012345");
    }

    @Test
    void elTotalTieneQueSerLaSumaDeLosMontosPorTasa() {
        assertThatThrownBy(() -> service.crear(compra(TipoComprobanteRg90.FACTURA, "001-001-0000123", "1000000")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tiene que ser la suma");
        verify(repository, never()).save(any());
    }

    @Test
    void elTicketDeCompraSoloInformaElTotalYNoDaCreditoFiscal() {
        when(currentUser.obtener()).thenReturn(new Usuario());
        when(repository.save(any(ComprobanteRegistrado.class))).thenAnswer(inv -> inv.getArgument(0));

        ComprobanteRegistrado c = service.crear(compra(TipoComprobanteRg90.TICKET, null, "85000"));

        assertThat(c.getGravado10()).isZero();
        assertThat(c.getIva10()).isZero();
        assertThat(c.getTotal()).isEqualByComparingTo("85000");
        assertThat(c.getNumero()).isNull();
    }

    @Test
    void juntaTodosLosErroresEnUnSoloMensaje() {
        ComprobanteRegistradoRequest malo = new ComprobanteRegistradoRequest(TipoRegistro.COMPRA,
                TipoComprobanteRg90.NOTA_CREDITO, LocalDate.of(2026, 11, 3), TipoIdentificacion.CEDULA, "1234567",
                null, "123", null, null, null, false, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000"),
                new BigDecimal("1000"), false, false, false, false, null, null, null);

        assertThatThrownBy(() -> service.crear(malo))
                .hasMessageContaining("RUC")
                .hasMessageContaining("timbrado es obligatorio")
                .hasMessageContaining("número de comprobante es obligatorio")
                .hasMessageContaining("al menos al IVA")
                .hasMessageContaining("comprobante que ajusta");
    }

    @Test
    void rechazaUnRucConDigitoVerificadorIncorrecto() {
        int dv = Modulo11.calcularDigitoVerificador("80012345");
        assertThat(ComprobanteRegistradoService.validarRuc("80012345-" + ((dv + 1) % 10)))
                .contains("debería ser " + dv);
        assertThat(ComprobanteRegistradoService.validarRuc(RUC_PROVEEDOR)).isNull();
        assertThat(ComprobanteRegistradoService.validarRuc("80012345")).isNull();
    }

    @Test
    void autofacturaYDespachoNoSonVentasYElDespachoVaConTimbradoCero() {
        ComprobanteRegistradoRequest autofacturaComoVenta = new ComprobanteRegistradoRequest(TipoRegistro.VENTA,
                TipoComprobanteRg90.AUTOFACTURA, LocalDate.of(2026, 11, 3), TipoIdentificacion.CEDULA, "1234567",
                "JUAN PEREZ", "12345678", "001-001-0000001", null, null, false, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.TEN, true, false, false, false, null, null, null);
        assertThatThrownBy(() -> service.crear(autofacturaComoVenta)).hasMessageContaining("no se puede registrar como venta");

        ComprobanteRegistrado despacho = new ComprobanteRegistrado();
        service.aplicar(despacho, new ComprobanteRegistradoRequest(TipoRegistro.COMPRA,
                TipoComprobanteRg90.DESPACHO_IMPORTACION, LocalDate.of(2026, 11, 3),
                TipoIdentificacion.IDENTIFICACION_TRIBUTARIA, "CN-998877", "SHENZHEN CO LTD", null, null, null, null,
                true, new BigDecimal("1100000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1100000"),
                true, true, false, false, null, null, null));
        assertThat(despacho.getTimbrado()).isEqualTo("0");
    }

    @Test
    void lasVentasElectronicasNoSeCarganAMano() {
        ComprobanteRegistradoRequest conCdc = new ComprobanteRegistradoRequest(TipoRegistro.VENTA,
                TipoComprobanteRg90.FACTURA, LocalDate.of(2026, 11, 3), TipoIdentificacion.RUC, RUC_PROVEEDOR, null,
                "12345678", "001-001-0000001", "0".repeat(44), null, false, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, true, false, false, false, null, null, null);

        assertThatThrownBy(() -> service.crear(conCdc)).hasMessageContaining("se toman del SIFEN");
    }

    @Test
    void noRegistraDosVecesElMismoComprobante() {
        when(repository.existsByTipoRegistroAndTipoComprobanteAndIdentificacionAndTimbradoAndNumero(
                TipoRegistro.COMPRA, TipoComprobanteRg90.FACTURA, RUC_PROVEEDOR, "12345678", "001-001-0000123"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(compra(TipoComprobanteRg90.FACTURA, "001-001-0000123", "1360000")))
                .hasMessageContaining("Ya está registrado");
    }

    @Test
    void noAdmiteComprobantesDeContadoAnterioresA2021() {
        ComprobanteRegistradoRequest viejo = new ComprobanteRegistradoRequest(TipoRegistro.COMPRA,
                TipoComprobanteRg90.FACTURA, LocalDate.of(2020, 12, 31), TipoIdentificacion.RUC, RUC_PROVEEDOR, null,
                "12345678", "001-001-0000001", null, CondicionPago.CONTADO, false, BigDecimal.TEN, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.TEN, true, false, false, false, null, null, null);

        assertThatThrownBy(() -> service.crear(viejo)).hasMessageContaining("01/01/2021");
    }
}
