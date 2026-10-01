package com.tottalstore.sifen.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
        log.setUsuario(usuario);
        log.setFactura(factura);
        return log;
    }

    @Test
    void listaSinFiltroUsaElListadoCompletoOrdenadoPorFecha() {
        LogAuditoria log = entrada("EMAIL_ENVIADO", null);
        when(repository.findAllByOrderByFechaHoraDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<?> resultado = controller.listar(null, 0, 20);

        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    void listaConFiltroDeFacturaUsaElMetodoFiltrado() {
        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(UUID.randomUUID());
        LogAuditoria log = entrada("REABRIR_RECHAZADO", factura);
        when(repository.findByFacturaIdOrderByFechaHoraDesc(eq(factura.getId()), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));

        Page<?> resultado = controller.listar(factura.getId(), 0, 20);

        assertThat(resultado.getContent()).hasSize(1);
    }
}
