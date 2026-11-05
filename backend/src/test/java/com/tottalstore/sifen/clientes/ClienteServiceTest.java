package com.tottalstore.sifen.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.dto.ClienteRequest;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.shared.CondicionIva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ValidadorRuc validadorRuc;

    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteService(clienteRepository, validadorRuc);
    }

    @Test
    void creaClienteCuandoRucEsValidoYNoExiste() {
        ClienteRequest request = new ClienteRequest(
                "80012345-6", "Distribuidora Sur SA", "Av. Mcal. López 1234", "admin@distribuidorasur.com.py",
                CondicionIva.RESPONSABLE_IVA, null);
        when(clienteRepository.existsById("80012345-6")).thenReturn(false);
        when(validadorRuc.validar("80012345-6")).thenReturn(ResultadoValidacionRuc.aceptado());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente creado = clienteService.crear(request);

        assertThat(creado.getRuc()).isEqualTo("80012345-6");
        assertThat(creado.isActivo()).isTrue();
    }

    @Test
    void rechazaRucDuplicado() {
        ClienteRequest request = new ClienteRequest(
                "80012345-6", "Distribuidora Sur SA", null, null, CondicionIva.RESPONSABLE_IVA, null);
        when(clienteRepository.existsById("80012345-6")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("80012345-6");
    }

    @Test
    void rechazaRucConFormatoInvalido() {
        ClienteRequest request = new ClienteRequest(
                "no-es-un-ruc", "Cliente cualquiera", null, null, CondicionIva.CONSUMIDOR_FINAL, null);
        when(clienteRepository.existsById("no-es-un-ruc")).thenReturn(false);
        when(validadorRuc.validar("no-es-un-ruc"))
                .thenReturn(ResultadoValidacionRuc.invalido("Formato inválido"));

        assertThatThrownBy(() -> clienteService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formato inválido");
    }

    @Test
    void editarActualizaLosDatosConservandoElRuc() {
        Cliente existente = new Cliente();
        existente.setRuc("80012345-6");
        existente.setRazonSocial("Nombre viejo");
        when(clienteRepository.findById("80012345-6")).thenReturn(java.util.Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente editado = clienteService.editar("80012345-6", new ClienteRequest(
                "80012345-6", "Nombre nuevo", null, "nuevo@cliente.com.py", CondicionIva.RESPONSABLE_IVA, null));

        assertThat(editado.getRuc()).isEqualTo("80012345-6");
        assertThat(editado.getRazonSocial()).isEqualTo("Nombre nuevo");
    }

    @Test
    void editarNoPermiteCambiarElRuc() {
        // BUG-003: cambiar el @Id de la entidad gestionada terminaba en un 500 de Hibernate.
        Cliente existente = new Cliente();
        existente.setRuc("80012345-6");
        when(clienteRepository.findById("80012345-6")).thenReturn(java.util.Optional.of(existente));

        assertThatThrownBy(() -> clienteService.editar("80012345-6", new ClienteRequest(
                        "80099999-1", "Otro", null, null, CondicionIva.RESPONSABLE_IVA, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no se puede modificar");
        assertThat(existente.getRuc()).isEqualTo("80012345-6");
    }
}
