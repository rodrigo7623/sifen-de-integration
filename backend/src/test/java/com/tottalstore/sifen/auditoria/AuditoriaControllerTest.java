package com.tottalstore.sifen.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auditoria.dto.AuditoriaResponse;
import com.tottalstore.sifen.auth.Rol;
import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class AuditoriaControllerTest {

    private final LogAuditoriaRepository repository = mock(LogAuditoriaRepository.class);
    private final AuditoriaController controller = new AuditoriaController(repository);

    private LogAuditoria entrada(String operacion, FacturaElectronica factura) {
        LogAuditoria log = new LogAuditoria();
        log.setId(UUID.randomUUID());
        log.setOperacion(operacion);
        log.setFechaHora(Instant.now());
        Usuario usuario = new Usuario();
        usuario.setNombre("Admin");
        usuario.setEmail("admin@tottalstore.com");
        usuario.setRol(Rol.ADMIN);
        log.setUsuario(usuario);
        log.setFactura(factura);
        return log;
    }

    @SuppressWarnings("unchecked")
    private void repositorioDevuelve(LogAuditoria... logs) {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(logs)));
    }

    @Test
    void listaSinFiltros() {
        repositorioDevuelve(entrada("EMAIL_ENVIADO", null));

        Page<AuditoriaResponse> resultado = controller.listar(null, null, null, null, null, null, null, 0, 20);

        assertThat(resultado.getContent()).singleElement().satisfies(r -> {
            assertThat(r.usuarioEmail()).isEqualTo("admin@tottalstore.com");
            assertThat(r.usuarioRol()).isEqualTo("ADMIN");
            assertThat(r.facturaId()).isNull();
        });
    }

    @Test
    void cadaEntradaTraeElNumeroDelComprobanteYElClienteEnVezDeSoloElId() {
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setCodigo("001");
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setCodigo("002");
        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-6");
        cliente.setRazonSocial("Distribuidora Sur SA");
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(UUID.randomUUID());
        factura.setTipoDoc(TipoDocumento.NC);
        factura.setEstablecimiento(establecimiento);
        factura.setPuntoExpedicion(punto);
        factura.setNumeroDocumento(5);
        factura.setEstadoDte(EstadoDte.APROBADO);
        factura.setCliente(cliente);
        factura.setTotalGeneral(new BigDecimal("150000"));
        repositorioDevuelve(entrada("APROBADO_SIFEN", factura));

        AuditoriaResponse r = controller.listar(null, null, null, null, null, null, "0000005", 0, 20).getContent().get(0);

        assertThat(r.documentoNumero()).isEqualTo("001-002-0000005");
        assertThat(r.documentoTipo()).isEqualTo(TipoDocumento.NC);
        assertThat(r.documentoEstado()).isEqualTo(EstadoDte.APROBADO);
        assertThat(r.clienteRazonSocial()).isEqualTo("Distribuidora Sur SA");
        assertThat(r.documentoTotal()).isEqualByComparingTo("150000");
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        assertThatThrownBy(() -> controller.listar(
                        null, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1), null, null, null, null, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anterior");
    }
}
