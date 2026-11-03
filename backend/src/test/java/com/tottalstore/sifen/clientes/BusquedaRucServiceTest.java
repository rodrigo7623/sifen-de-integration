package com.tottalstore.sifen.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.dto.ConsultaRucClienteResponse;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.shared.CondicionIva;
import com.tottalstore.sifen.sifen.ConsultaRucResult;
import com.tottalstore.sifen.sifen.ConsultaRucSifenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class BusquedaRucServiceTest {

    private ClienteRepository clienteRepository;
    private ConsultaRucSifenService sifen;
    private ObjectProvider<ConsultaRucSifenService> proveedor;
    private BusquedaRucService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        sifen = mock(ConsultaRucSifenService.class);
        proveedor = mock(ObjectProvider.class);
        when(proveedor.getIfAvailable()).thenReturn(sifen);
        when(clienteRepository.findById(any())).thenReturn(Optional.empty());
        service = new BusquedaRucService(clienteRepository, proveedor);
    }

    @Test
    void sinDigitoVerificadorLoCalculaYTraeLosDatosDelSifen() {
        when(sifen.consultar("4309652")).thenReturn(new ConsultaRucResult(
                true, "0502", "RUC encontrado", "4309652", "RAMIREZ MEDINA, BARTOLOME ", "ACT", "ACTIVO", false));

        ConsultaRucClienteResponse r = service.buscar(" 4.309.652 ");

        assertThat(r.ruc()).isEqualTo("4309652-2");
        assertThat(r.dvCalculado()).isTrue();
        assertThat(r.cliente()).isNull();
        assertThat(r.existeEnSifen()).isTrue();
        assertThat(r.razonSocial()).isEqualTo("RAMIREZ MEDINA, BARTOLOME");
        assertThat(r.estado()).isEqualTo("ACTIVO");
        assertThat(r.facturadorElectronico()).isFalse();
        assertThat(r.condicionIvaSugerida()).isEqualTo(CondicionIva.RESPONSABLE_IVA);
    }

    @Test
    void siYaEsClienteNoConsultaAlSifen() {
        Cliente cliente = new Cliente();
        cliente.setRuc("4309652-2");
        cliente.setRazonSocial("Cliente registrado");
        cliente.setCondicionIva(CondicionIva.PEQUENO_CONTRIBUYENTE);
        cliente.setActivo(true);
        when(clienteRepository.findById("4309652-2")).thenReturn(Optional.of(cliente));

        ConsultaRucClienteResponse r = service.buscar("4309652-2");

        assertThat(r.cliente()).isNotNull();
        assertThat(r.razonSocial()).isEqualTo("Cliente registrado");
        verify(sifen, never()).consultar(any());
    }

    @Test
    void rucInexistenteEnElSifenSugiereConsumidorFinal() {
        when(sifen.consultar("1234567")).thenReturn(
                new ConsultaRucResult(true, "0500", "RUC no existe", null, null, null, null, null));

        ConsultaRucClienteResponse r = service.buscar("1234567");

        assertThat(r.existeEnSifen()).isFalse();
        assertThat(r.razonSocial()).isNull();
        assertThat(r.condicionIvaSugerida()).isEqualTo(CondicionIva.CONSUMIDOR_FINAL);
        assertThat(r.mensaje()).contains("no figura en el SIFEN");
    }

    @Test
    void siElSifenNoRespondeSigueConLaValidacionLocal() {
        when(sifen.consultar("4309652")).thenReturn(
                new ConsultaRucResult(false, "ERROR_CONEXION", "timeout", null, null, null, null, null));

        ConsultaRucClienteResponse r = service.buscar("4309652-2");

        assertThat(r.ruc()).isEqualTo("4309652-2");
        assertThat(r.consultadoEnSifen()).isFalse();
        assertThat(r.existeEnSifen()).isNull();
        assertThat(r.mensaje()).contains("No se pudo consultar al SIFEN");
    }

    @Test
    void enModoSimuladoNoHayConsulta() {
        when(proveedor.getIfAvailable()).thenReturn(null);

        ConsultaRucClienteResponse r = service.buscar("4309652");

        assertThat(r.ruc()).isEqualTo("4309652-2");
        assertThat(r.consultadoEnSifen()).isFalse();
        assertThat(r.mensaje()).contains("modo simulado");
    }

    @Test
    void rechazaDigitoVerificadorIncorrectoYFormatoInvalido() {
        assertThatThrownBy(() -> service.buscar("4309652-5"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dígito verificador");
        assertThatThrownBy(() -> service.buscar("ABC"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formato inválido");
    }
}
