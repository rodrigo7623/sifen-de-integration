package com.tottalstore.sifen.dashboard;

import com.tottalstore.sifen.dashboard.dto.ResumenDashboardResponse;
import com.tottalstore.sifen.dashboard.dto.ResumenDashboardResponse.RechazoResumen;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * KPIs de la pantalla de inicio. Calculados en memoria sobre {@code FacturaRepository.findAll()}
 * -- volumen de datos esperado (proyecto académico) no justifica agregaciones SQL dedicadas todavía;
 * si el volumen creciera, esa es la optimización obvia.
 */
@Service
public class DashboardService {

    private static final int CANTIDAD_ULTIMOS_RECHAZADOS = 5;

    private final FacturaRepository facturaRepository;

    public DashboardService(FacturaRepository facturaRepository) {
        this.facturaRepository = facturaRepository;
    }

    public ResumenDashboardResponse resumen() {
        List<FacturaElectronica> facturas = facturaRepository.findAll();
        YearMonth mesActual = YearMonth.now();

        Map<EstadoDte, Long> facturasPorEstado = facturas.stream()
                .collect(Collectors.groupingBy(FacturaElectronica::getEstadoDte, Collectors.counting()));

        List<FacturaElectronica> facturasDelMes = facturas.stream()
                .filter(f -> mesActual.equals(YearMonth.from(
                        f.getFechaEmision().atZone(ZoneId.systemDefault()).toLocalDate())))
                .toList();

        BigDecimal totalFacturadoMes = facturasDelMes.stream()
                .filter(f -> f.getEstadoDte() == EstadoDte.APROBADO)
                .map(FacturaElectronica::getTotalGeneral)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RechazoResumen> ultimosRechazados = facturas.stream()
                .filter(f -> f.getEstadoDte() == EstadoDte.RECHAZADO)
                .sorted(Comparator.comparing(FacturaElectronica::getFechaEmision).reversed())
                .limit(CANTIDAD_ULTIMOS_RECHAZADOS)
                .map(f -> new RechazoResumen(
                        f.getId(),
                        f.getCliente().getRazonSocial(),
                        f.getRespuestaSifen() != null ? f.getRespuestaSifen().getDescripcion() : null,
                        f.getFechaEmision()))
                .toList();

        return new ResumenDashboardResponse(
                facturasPorEstado, facturasDelMes.size(), totalFacturadoMes, ultimosRechazados);
    }
}
