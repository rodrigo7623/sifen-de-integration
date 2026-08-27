package com.tottalstore.sifen.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.catalogo.dto.ProductoRequest;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.shared.TasaIva;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    private ProductoService productoService;

    @BeforeEach
    void setUp() {
        productoService = new ProductoService(productoRepository);
    }

    @Test
    void creaProductoCuandoElCodigoNoExiste() {
        ProductoRequest request = new ProductoRequest("TLD-220V", "Taladro percutor", "UN",
                new BigDecimal("350000"), TasaIva.DIEZ);
        when(productoRepository.existsById("TLD-220V")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto creado = productoService.crear(request);

        assertThat(creado.getCodigo()).isEqualTo("TLD-220V");
        assertThat(creado.isActivo()).isTrue();
    }

    @Test
    void rechazaCodigoDuplicado() {
        ProductoRequest request = new ProductoRequest("TLD-220V", "Taladro percutor", "UN",
                new BigDecimal("350000"), TasaIva.DIEZ);
        when(productoRepository.existsById("TLD-220V")).thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TLD-220V");
    }

    @Test
    void rechazaPrecioNoPositivo() {
        ProductoRequest request = new ProductoRequest("TLD-220V", "Taladro percutor", "UN",
                new BigDecimal("0"), TasaIva.DIEZ);
        when(productoRepository.existsById("TLD-220V")).thenReturn(false);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("precio");
    }
}
