package com.tottalstore.sifen.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.configuracion.dto.ActividadEconomicaRequest;
import com.tottalstore.sifen.configuracion.dto.ConfiguracionEmisorRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfiguracionEmisorServiceTest {

    private final ConfiguracionEmisorRepository repository = mock(ConfiguracionEmisorRepository.class);
    private final ConfiguracionEmisorService service = new ConfiguracionEmisorService(repository);

    @Test
    void creaLaFilaUnicaSiNoExisteTodavia() {
        when(repository.findAll()).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionEmisor creada = service.obtenerOCrear();

        assertThat(creada.getId()).isNotNull();
    }

    @Test
    void reutilizaLaFilaExistenteEnVezDeCrearOtra() {
        ConfiguracionEmisor existente = new ConfiguracionEmisor();
        existente.setId(UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of(existente));

        ConfiguracionEmisor obtenida = service.obtenerOCrear();

        assertThat(obtenida).isSameAs(existente);
    }

    @Test
    void actualizarPersisteTodosLosCamposDelRequest() {
        ConfiguracionEmisor existente = new ConfiguracionEmisor();
        existente.setId(UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ConfiguracionEmisorRequest(
                "Tottal Store", "Tottal", "Avda. Mcal. López", "1234", null, null,
                "11", "ASUNCION", null, null, "1", "ASUNCION",
                "021123456", "ventas@tottalstore.com.py", "47111", "Venta al por menor",
                "4309652", 2, 1,
                null, null, null, null);

        ConfiguracionEmisor actualizada = service.actualizar(request);

        assertThat(actualizada.getRazonSocial()).isEqualTo("Tottal Store");
        assertThat(actualizada.getRucBase()).isEqualTo("4309652");
        assertThat(actualizada.getDvRuc()).isEqualTo(2);
        assertThat(actualizada.getTimbradoFechaInicio()).isNull();
    }

    @Test
    void actualizarAceptaFechasDeTimbradoCuandoSeInforman() {
        ConfiguracionEmisor existente = new ConfiguracionEmisor();
        existente.setId(UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new ConfiguracionEmisorRequest(
                "Tottal Store", null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                "4309652", 2, 1,
                "12345678", LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), null);

        ConfiguracionEmisor actualizada = service.actualizar(request);

        assertThat(actualizada.getTimbradoNumero()).isEqualTo("12345678");
        assertThat(actualizada.getTimbradoFechaFin()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    private static ConfiguracionEmisorRequest conActividades(List<ActividadEconomicaRequest> actividades) {
        return new ConfiguracionEmisorRequest(
                "Tottal Store", null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null,
                "4309652", 2, 1,
                null, null, null, actividades);
    }

    private ConfiguracionEmisor existente() {
        ConfiguracionEmisor existente = new ConfiguracionEmisor();
        existente.setId(UUID.randomUUID());
        when(repository.findAll()).thenReturn(List.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return existente;
    }

    @Test
    void variasActividadesConLaPrincipalCopiadaALasColumnasDeSiempre() {
        existente();

        ConfiguracionEmisor actualizada = service.actualizar(conActividades(List.of(
                new ActividadEconomicaRequest("47521", "Ferretería", false),
                new ActividadEconomicaRequest("47890", "Comercio en puestos de venta", true))));

        assertThat(actualizada.getActividadesEconomicas()).hasSize(2);
        assertThat(actualizada.getActividadEconomicaCodigo()).isEqualTo("47890");
        assertThat(actualizada.actividadesOrdenadas()).extracting(ActividadEconomicaEmisor::getCodigo)
                .containsExactly("47890", "47521");
    }

    @Test
    void cambiarLaPrincipalReutilizaLasFilasYQuitaLasQueYaNoEstan() {
        ConfiguracionEmisor config = existente();
        service.actualizar(conActividades(List.of(
                new ActividadEconomicaRequest("47521", "Ferretería", true),
                new ActividadEconomicaRequest("47890", "Puestos de venta", false),
                new ActividadEconomicaRequest("46900", "Mayorista", false))));
        ActividadEconomicaEmisor ferreteria = config.getActividadesEconomicas().get(0);

        service.actualizar(conActividades(List.of(
                new ActividadEconomicaRequest("47521", "Ferretería", false),
                new ActividadEconomicaRequest("47890", "Puestos de venta", true))));

        assertThat(config.getActividadesEconomicas()).hasSize(2).contains(ferreteria);
        assertThat(ferreteria.isPrincipal()).isFalse();
        assertThat(config.getActividadEconomicaCodigo()).isEqualTo("47890");
    }

    @Test
    void exigeExactamenteUnaPrincipal() {
        existente();

        assertThatThrownBy(() -> service.actualizar(conActividades(List.of(
                new ActividadEconomicaRequest("47521", "Ferretería", false)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("principal");
        assertThatThrownBy(() -> service.actualizar(conActividades(List.of(
                new ActividadEconomicaRequest("47521", "Ferretería", true),
                new ActividadEconomicaRequest("47890", "Puestos", true)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("principal");
    }

    @Test
    void rechazaUnaActividadRepetida() {
        existente();

        assertThatThrownBy(() -> service.actualizar(conActividades(List.of(
                new ActividadEconomicaRequest("47521", "Ferretería", true),
                new ActividadEconomicaRequest("47521", "Otra vez", false)))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("repetida");
    }

    @Test
    void sinListaUsaElParCodigoDescripcionComoUnicaActividad() {
        existente();
        var request = new ConfiguracionEmisorRequest(
                "Tottal Store", null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, "47111", "Venta al por menor",
                "4309652", 2, 1,
                null, null, null, null);

        ConfiguracionEmisor actualizada = service.actualizar(request);

        assertThat(actualizada.getActividadesEconomicas()).singleElement()
                .satisfies(a -> assertThat(a.isPrincipal()).isTrue());
        assertThat(actualizada.getActividadEconomicaCodigo()).isEqualTo("47111");
    }
}
