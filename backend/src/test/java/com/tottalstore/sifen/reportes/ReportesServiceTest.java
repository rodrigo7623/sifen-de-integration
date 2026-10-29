package com.tottalstore.sifen.reportes;

import static org.assertj.core.api.Assertions.assertThat;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.reportes.dto.HistorialClienteResponse;
import com.tottalstore.sifen.reportes.dto.ReporteRechazosResponse;
import com.tottalstore.sifen.sifen.IntentoEnvioSifen;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportesServiceTest {

    private final Cliente cliente = cliente();

    @Test
    void agrupaRechazosPorCodigoYPorMes() {
        FacturaElectronica a = factura(EstadoDte.APROBADO, "1000");
        FacturaElectronica b = factura(EstadoDte.RECHAZADO, "2000");
        List<IntentoEnvioSifen> intentos = List.of(
                intento(a, "2026-09-10T12:00:00Z", false, "0160", "XML malformado [dBasExe]"),
                intento(a, "2026-10-01T12:00:00Z", false, "0160", "No se informó el schema"),
                intento(a, "2026-10-02T12:00:00Z", true, "0260", "Aprobado"),
                intento(b, "2026-10-03T12:00:00Z", false, "1101", "Número de timbrado inválido"));

        ReporteRechazosResponse r = ReportesService.calcularRechazos(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31), intentos);

        assertThat(r.totalIntentos()).isEqualTo(4);
        assertThat(r.aprobados()).isEqualTo(1);
        assertThat(r.rechazados()).isEqualTo(3);
        assertThat(r.tasaAprobacion()).isEqualTo(25.0);
        assertThat(r.facturasConRechazo()).isEqualTo(2);

        assertThat(r.porCodigo()).extracting(ReporteRechazosResponse.PorCodigo::codigo).containsExactly("0160", "1101");
        assertThat(r.porCodigo().get(0).cantidad()).isEqualTo(2);
        assertThat(r.porCodigo().get(0).mensaje()).isEqualTo("No se informó el schema");

        assertThat(r.porMes()).extracting(ReporteRechazosResponse.PorMes::mes).containsExactly("2026-09", "2026-10");
        assertThat(r.porMes().get(1).intentos()).isEqualTo(3);
        assertThat(r.porMes().get(1).rechazados()).isEqualTo(2);

        assertThat(r.ultimosRechazos().get(0).codigo()).isEqualTo("1101");
    }

    @Test
    void sinIntentosLaTasaEsNullYLosMesesAparecenEnCero() {
        ReporteRechazosResponse r = ReportesService.calcularRechazos(
                LocalDate.of(2026, 8, 15), LocalDate.of(2026, 10, 2), List.of());

        assertThat(r.tasaAprobacion()).isNull();
        assertThat(r.porMes()).hasSize(3).allMatch(m -> m.intentos() == 0);
    }

    @Test
    void historialDelClienteResumeFacturasEIntentos() {
        FacturaElectronica aprobada = factura(EstadoDte.APROBADO, "150000");
        FacturaElectronica rechazada = factura(EstadoDte.RECHAZADO, "80000");
        FacturaElectronica borrador = factura(EstadoDte.BORRADOR, "1000");
        List<IntentoEnvioSifen> intentos = List.of(
                intento(aprobada, "2026-10-01T12:00:00Z", false, "0160", "XML malformado"),
                intento(aprobada, "2026-10-01T13:00:00Z", true, "0260", "Aprobado"),
                intento(rechazada, "2026-10-02T12:00:00Z", false, "1101", "Timbrado inválido"));

        HistorialClienteResponse h = ReportesService.calcularHistorial(
                cliente, List.of(aprobada, rechazada, borrador), intentos);

        assertThat(h.cantidadFacturas()).isEqualTo(3);
        assertThat(h.aprobadas()).isEqualTo(1);
        assertThat(h.rechazadas()).isEqualTo(1);
        assertThat(h.pendientes()).isEqualTo(1);
        assertThat(h.montoAprobado()).isEqualByComparingTo("150000");
        assertThat(h.intentosEnvio()).isEqualTo(3);
        assertThat(h.intentosRechazados()).isEqualTo(2);
        assertThat(h.facturas().get(0).intentos()).isEqualTo(2);
        assertThat(h.facturas().get(0).ultimoCodigo()).isEqualTo("0260");
        assertThat(h.facturas().get(2).intentos()).isZero();
        assertThat(h.facturas().get(2).ultimoCodigo()).isNull();
    }

    private FacturaElectronica factura(EstadoDte estado, String total) {
        FacturaElectronica f = new FacturaElectronica();
        f.setId(UUID.randomUUID());
        f.setCliente(cliente);
        f.setEstadoDte(estado);
        f.setTotalGeneral(new BigDecimal(total));
        return f;
    }

    private static IntentoEnvioSifen intento(
            FacturaElectronica f, String fecha, boolean aprobado, String codigo, String descripcion) {
        IntentoEnvioSifen i = new IntentoEnvioSifen();
        i.setFactura(f);
        i.setFecha(Instant.parse(fecha));
        i.setAprobado(aprobado);
        i.setCodigo(codigo);
        i.setDescripcion(descripcion);
        return i;
    }

    private static Cliente cliente() {
        Cliente c = new Cliente();
        c.setRuc("80012345-6");
        c.setRazonSocial("CLIENTE DE PRUEBA S.A.");
        return c;
    }
}
