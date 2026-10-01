package com.tottalstore.sifen.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.dashboard.dto.ResumenDashboardResponse;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaRepository;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private final FacturaRepository facturaRepository = mock(FacturaRepository.class);
    private final DashboardService service = new DashboardService(facturaRepository);

    private FacturaElectronica factura(EstadoDte estado, BigDecimal total, String motivoRechazo) {
        Cliente cliente = new Cliente();
        cliente.setRazonSocial("Cliente de prueba");

        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(UUID.randomUUID());
        factura.setEstadoDte(estado);
        factura.setTotalGeneral(total);
        factura.setFechaEmision(Instant.now());
        factura.setCliente(cliente);
        if (motivoRechazo != null) {
            RespuestaSifen respuesta = new RespuestaSifen();
            respuesta.setDescripcion(motivoRechazo);
            factura.setRespuestaSifen(respuesta);
        }
        return factura;
    }

    @Test
    void agrupaPorEstadoYSumaSoloLoAprobadoDelMesActual() {
        when(facturaRepository.findAll()).thenReturn(List.of(
                factura(EstadoDte.APROBADO, new BigDecimal("100000"), null),
                factura(EstadoDte.APROBADO, new BigDecimal("50000"), null),
                factura(EstadoDte.RECHAZADO, new BigDecimal("30000"), "XML malformado"),
                factura(EstadoDte.BORRADOR, new BigDecimal("10000"), null)));

        ResumenDashboardResponse resumen = service.resumen();

        assertThat(resumen.facturasPorEstado().get(EstadoDte.APROBADO)).isEqualTo(2L);
        assertThat(resumen.facturasPorEstado().get(EstadoDte.RECHAZADO)).isEqualTo(1L);
        assertThat(resumen.totalFacturadoMes()).isEqualByComparingTo("150000");
        assertThat(resumen.cantidadFacturasMes()).isEqualTo(4);
        assertThat(resumen.ultimosRechazados()).hasSize(1);
        assertThat(resumen.ultimosRechazados().get(0).motivo()).isEqualTo("XML malformado");
    }

    @Test
    void noExplotaSinFacturas() {
        when(facturaRepository.findAll()).thenReturn(List.of());

        ResumenDashboardResponse resumen = service.resumen();

        assertThat(resumen.facturasPorEstado()).isEmpty();
        assertThat(resumen.totalFacturadoMes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.ultimosRechazados()).isEmpty();
    }
}
