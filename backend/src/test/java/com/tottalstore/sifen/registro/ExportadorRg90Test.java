package com.tottalstore.sifen.registro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.CondicionPago;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportadorRg90Test {

    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);

    @Mock
    private ComprobanteRegistradoRepository repository;
    @Mock
    private ConfiguracionEmisorService configuracionEmisorService;

    private ExportadorRg90 exportador;

    @BeforeEach
    void setUp() {
        exportador = new ExportadorRg90(repository, configuracionEmisorService);
        ConfiguracionEmisor emisor = new ConfiguracionEmisor();
        emisor.setRucBase("4309652");
        emisor.setDvRuc(2);
        // lenient: los tests de una sola fila no leen la configuración.
        org.mockito.Mockito.lenient().when(configuracionEmisorService.obtenerOCrear()).thenReturn(emisor);
    }

    static ComprobanteRegistrado compra(String numero) {
        ComprobanteRegistrado c = new ComprobanteRegistrado();
        c.setTipoRegistro(TipoRegistro.COMPRA);
        c.setTipoComprobante(TipoComprobanteRg90.FACTURA);
        c.setFechaEmision(LocalDate.of(2026, 11, 5));
        c.setTipoIdentificacion(TipoIdentificacion.RUC);
        c.setIdentificacion("80024627-1");
        c.setRazonSocial("MINISTERIO DE HACIENDA");
        c.setTimbrado("11223369");
        c.setNumero(numero);
        c.setGravado10(new BigDecimal("100000"));
        c.setTotal(new BigDecimal("100000"));
        c.setImputaIva(true);
        c.setImputaIre(true);
        return c;
    }

    private void periodo(List<ComprobanteRegistrado> ventas, List<ComprobanteRegistrado> compras) {
        when(repository.delPeriodo(eq(TipoRegistro.VENTA), any(), any())).thenReturn(ventas);
        when(repository.delPeriodo(eq(TipoRegistro.COMPRA), any(), any())).thenReturn(compras);
    }

    @Test
    void laFilaDeCompraSigueElOrdenDeLaEspecificacion() {
        // Mismo comprobante que el ejemplo TXT del anexo de la Especificación Técnica (con NO IMPUTA = N).
        assertThat(ExportadorRg90.fila(compra("001-002-0000250")).split("\t", -1)).containsExactly(
                "2", "11", "80024627", "MINISTERIO DE HACIENDA", "109", "05/11/2026", "11223369", "001-002-0000250",
                "100000", "0", "0", "100000", "1", "N", "S", "S", "N", "N", "", "");
    }

    @Test
    void laNotaDeCreditoDeVentaVaEnPositivoConSuComprobanteAsociadoYSinNoImputa() {
        ComprobanteRegistrado nc = compra("001-002-0000011");
        nc.setTipoRegistro(TipoRegistro.VENTA);
        nc.setTipoComprobante(TipoComprobanteRg90.NOTA_CREDITO);
        nc.setCondicion(CondicionPago.CREDITO);
        nc.setAsociadoNumero("001-012-0001240");
        nc.setAsociadoTimbrado("87654321");

        String[] campos = ExportadorRg90.fila(nc).split("\t", -1);

        assertThat(campos).hasSize(19);
        assertThat(campos[0]).isEqualTo("1");
        assertThat(campos[4]).isEqualTo("110");
        assertThat(campos[11]).isEqualTo("100000");
        assertThat(campos[12]).isEqualTo("2");
        assertThat(campos[17]).isEqualTo("001-012-0001240");
        assertThat(campos[18]).isEqualTo("87654321");
    }

    @Test
    void unTabuladorEnLaRazonSocialNoCorreLasColumnas() {
        ComprobanteRegistrado c = compra("001-002-0000250");
        c.setRazonSocial("EMPRESA\tCON\nTAB");

        assertThat(ExportadorRg90.fila(c).split("\t", -1)).hasSize(20).contains("EMPRESA CON TAB");
    }

    @Test
    void elZipSeLlamaComoElTxtYExcluyeLosElectronicos() throws Exception {
        ComprobanteRegistrado electronica = compra("001-002-0000999");
        electronica.setCdc("0".repeat(44));
        periodo(List.of(), List.of(compra("001-002-0000250"), electronica));
        when(repository.siguienteNumeroExportacion()).thenReturn(7L);

        ExportadorRg90.Archivo archivo = exportador.exportar(NOVIEMBRE);

        assertThat(archivo.nombre()).isEqualTo("4309652_REG_112026_T0007.zip");
        assertThat(archivo.comprobantes()).isEqualTo(1);
        Map<String, String> entradas = descomprimir(archivo.contenido());
        assertThat(entradas).containsOnlyKeys("4309652_REG_112026_T0007.txt");
        String txt = entradas.get("4309652_REG_112026_T0007.txt");
        assertThat(txt).contains("001-002-0000250").doesNotContain("0000999");
        assertThat(txt.split("\r\n")).hasSize(1);
    }

    @Test
    void conMasDeCincoMilFilasArmaUnZipPorLote() throws Exception {
        List<ComprobanteRegistrado> muchas = new ArrayList<>();
        for (int i = 1; i <= ExportadorRg90.MAXIMO_FILAS + 1; i++) {
            muchas.add(compra(String.format("001-002-%07d", i)));
        }
        periodo(List.of(), muchas);
        when(repository.siguienteNumeroExportacion()).thenReturn(1L, 2L);

        ExportadorRg90.Archivo archivo = exportador.exportar(NOVIEMBRE);

        assertThat(descomprimir(archivo.contenido()).keySet())
                .containsExactly("4309652_REG_112026_T0001.zip", "4309652_REG_112026_T0002.zip");
        assertThat(archivo.comprobantes()).isEqualTo(5001);
    }

    @Test
    void sinComprobantesNoElectronicosAvisaEnVezDeArmarUnArchivoVacio() {
        periodo(List.of(), List.of());

        assertThatThrownBy(() -> exportador.exportar(NOVIEMBRE)).hasMessageContaining("los electrónicos ya los tiene la SET");
    }

    private static Map<String, String> descomprimir(byte[] zip) throws Exception {
        Map<String, String> entradas = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                entradas.put(e.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return entradas;
    }
}
