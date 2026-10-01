package com.tottalstore.sifen.establecimientos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.establecimientos.dto.EstablecimientoRequest;
import com.tottalstore.sifen.establecimientos.dto.PuntoExpedicionRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EstablecimientoServiceTest {

    private final EstablecimientoRepository establecimientoRepository = mock(EstablecimientoRepository.class);
    private final PuntoExpedicionRepository puntoExpedicionRepository = mock(PuntoExpedicionRepository.class);
    private final EstablecimientoService service =
            new EstablecimientoService(establecimientoRepository, puntoExpedicionRepository);

    private EstablecimientoRequest requestEjemplo(String codigo) {
        return new EstablecimientoRequest(
                codigo, "Sucursal de prueba", null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void creaUnEstablecimientoNuevo() {
        when(establecimientoRepository.existsByCodigo("002")).thenReturn(false);
        when(establecimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Establecimiento creado = service.crear(requestEjemplo("002"));

        assertThat(creado.getCodigo()).isEqualTo("002");
        assertThat(creado.getDenominacion()).isEqualTo("Sucursal de prueba");
    }

    @Test
    void noPermiteDosEstablecimientosConElMismoCodigo() {
        when(establecimientoRepository.existsByCodigo("001")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(requestEjemplo("001")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void agregaUnPuntoDeExpedicionAUnEstablecimientoExistente() {
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setId(UUID.randomUUID());
        establecimiento.setCodigo("001");
        when(establecimientoRepository.findById(establecimiento.getId()))
                .thenReturn(java.util.Optional.of(establecimiento));
        when(puntoExpedicionRepository.existsByEstablecimientoIdAndCodigo(establecimiento.getId(), "002"))
                .thenReturn(false);
        when(puntoExpedicionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Establecimiento actualizado = service.agregarPuntoExpedicion(
                establecimiento.getId(), new PuntoExpedicionRequest("002", "Caja 2"));

        assertThat(actualizado.getPuntosExpedicion()).hasSize(1);
        assertThat(actualizado.getPuntosExpedicion().get(0).getCodigo()).isEqualTo("002");
    }

    @Test
    void noPermiteDosPuntosDeExpedicionConElMismoCodigoEnElMismoEstablecimiento() {
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setId(UUID.randomUUID());
        establecimiento.setCodigo("001");
        when(establecimientoRepository.findById(establecimiento.getId()))
                .thenReturn(java.util.Optional.of(establecimiento));
        when(puntoExpedicionRepository.existsByEstablecimientoIdAndCodigo(establecimiento.getId(), "001"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.agregarPuntoExpedicion(
                        establecimiento.getId(), new PuntoExpedicionRequest("001", "Caja 1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listarSinInactivosUsaElRepositorioFiltrado() {
        when(establecimientoRepository.findByActivoTrueOrderByCodigoAsc()).thenReturn(List.of());

        service.listar(false);

        org.mockito.Mockito.verify(establecimientoRepository).findByActivoTrueOrderByCodigoAsc();
    }
}
