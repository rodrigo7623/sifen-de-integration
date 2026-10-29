package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.clientes.ClienteRepository;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaRepository;
import com.tottalstore.sifen.reportes.dto.HistorialClienteResponse;
import com.tottalstore.sifen.reportes.dto.HistorialClienteResponse.FacturaCliente;
import com.tottalstore.sifen.reportes.dto.ReporteRechazosResponse;
import com.tottalstore.sifen.reportes.dto.ReporteRechazosResponse.PorCodigo;
import com.tottalstore.sifen.reportes.dto.ReporteRechazosResponse.PorMes;
import com.tottalstore.sifen.reportes.dto.ReporteRechazosResponse.Rechazo;
import com.tottalstore.sifen.sifen.IntentoEnvioSifen;
import com.tottalstore.sifen.sifen.IntentoEnvioSifenRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Estadísticas de rechazo e historial por cliente (Release 3). Como el dashboard, se calculan en
 * memoria: el volumen esperado no justifica agregaciones SQL dedicadas.
 */
@Service
public class ReportesService {

    private static final int CANTIDAD_ULTIMOS_RECHAZOS = 20;
    private static final int MESES_POR_DEFECTO = 6;

    private final IntentoEnvioSifenRepository intentoRepository;
    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;

    public ReportesService(
            IntentoEnvioSifenRepository intentoRepository,
            FacturaRepository facturaRepository,
            ClienteRepository clienteRepository) {
        this.intentoRepository = intentoRepository;
        this.facturaRepository = facturaRepository;
        this.clienteRepository = clienteRepository;
    }

    /** Rango inclusivo de fechas; por defecto, los últimos seis meses calendario incluido el actual. */
    public ReporteRechazosResponse rechazos(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta : LocalDate.now(LibroIvaService.ZONA);
        LocalDate inicio = desde != null ? desde : YearMonth.from(fin).minusMonths(MESES_POR_DEFECTO - 1).atDay(1);
        if (inicio.isAfter(fin)) {
            throw new BusinessException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        List<IntentoEnvioSifen> intentos = intentoRepository.findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaAsc(
                inicio.atStartOfDay(LibroIvaService.ZONA).toInstant(),
                fin.plusDays(1).atStartOfDay(LibroIvaService.ZONA).toInstant());
        return calcularRechazos(inicio, fin, intentos);
    }

    static ReporteRechazosResponse calcularRechazos(LocalDate inicio, LocalDate fin, List<IntentoEnvioSifen> intentos) {
        long aprobados = intentos.stream().filter(IntentoEnvioSifen::isAprobado).count();
        List<IntentoEnvioSifen> rechazados = intentos.stream().filter(i -> !i.isAprobado()).toList();

        List<PorCodigo> porCodigo = rechazados.stream()
                .collect(Collectors.groupingBy(i -> i.getCodigo() != null ? i.getCodigo() : "SIN_CODIGO"))
                .entrySet().stream()
                .map(e -> {
                    // Mensaje del intento más reciente con ese código: el texto del SIFEN puede variar
                    // (por ejemplo, 0160 detalla el campo que falla).
                    IntentoEnvioSifen ultimo = e.getValue().get(e.getValue().size() - 1);
                    return new PorCodigo(e.getKey(), ultimo.getDescripcion(), e.getValue().size(), ultimo.getFecha());
                })
                .sorted(Comparator.comparingLong(PorCodigo::cantidad).reversed().thenComparing(PorCodigo::codigo))
                .toList();

        Map<YearMonth, List<IntentoEnvioSifen>> porMesMap = new TreeMap<>();
        for (YearMonth m = YearMonth.from(inicio); !m.isAfter(YearMonth.from(fin)); m = m.plusMonths(1)) {
            porMesMap.put(m, new java.util.ArrayList<>());
        }
        intentos.forEach(i -> porMesMap
                .computeIfAbsent(YearMonth.from(i.getFecha().atZone(LibroIvaService.ZONA)), k -> new java.util.ArrayList<>())
                .add(i));
        List<PorMes> porMes = porMesMap.entrySet().stream()
                .map(e -> {
                    long ok = e.getValue().stream().filter(IntentoEnvioSifen::isAprobado).count();
                    return new PorMes(e.getKey().toString(), e.getValue().size(), ok, e.getValue().size() - ok);
                })
                .toList();

        List<Rechazo> ultimos = rechazados.reversed().stream()
                .limit(CANTIDAD_ULTIMOS_RECHAZOS)
                .map(i -> new Rechazo(
                        i.getFactura().getId(),
                        i.getFecha(),
                        i.getFactura().getCliente().getRuc(),
                        i.getFactura().getCliente().getRazonSocial(),
                        i.getCodigo(),
                        i.getDescripcion()))
                .toList();

        long facturasConRechazo = rechazados.stream().map(i -> i.getFactura().getId()).distinct().count();
        Double tasa = intentos.isEmpty() ? null : Math.round(aprobados * 1000.0 / intentos.size()) / 10.0;

        return new ReporteRechazosResponse(
                inicio, fin, intentos.size(), aprobados, rechazados.size(), tasa, facturasConRechazo,
                porCodigo, porMes, ultimos);
    }

    public HistorialClienteResponse historialCliente(String ruc) {
        Cliente cliente = clienteRepository.findById(ruc)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + ruc));
        List<FacturaElectronica> facturas = facturaRepository.findByClienteRucOrderByFechaEmisionDesc(ruc);
        List<IntentoEnvioSifen> intentos = intentoRepository.findByFacturaClienteRucOrderByFechaAsc(ruc);
        return calcularHistorial(cliente, facturas, intentos);
    }

    static HistorialClienteResponse calcularHistorial(
            Cliente cliente, List<FacturaElectronica> facturas, List<IntentoEnvioSifen> intentos) {
        Map<UUID, List<IntentoEnvioSifen>> intentosPorFactura =
                intentos.stream().collect(Collectors.groupingBy(i -> i.getFactura().getId()));

        List<FacturaCliente> detalle = facturas.stream()
                .map(f -> {
                    List<IntentoEnvioSifen> deLaFactura = intentosPorFactura.getOrDefault(f.getId(), List.of());
                    IntentoEnvioSifen ultimo = deLaFactura.isEmpty() ? null : deLaFactura.get(deLaFactura.size() - 1);
                    return new FacturaCliente(
                            f.getId(),
                            f.getFechaEmision(),
                            LibroIvaService.numeroComprobante(f),
                            f.getEstadoDte(),
                            f.getTotalGeneral(),
                            deLaFactura.size(),
                            ultimo != null ? ultimo.getCodigo() : null,
                            ultimo != null ? ultimo.getDescripcion() : null);
                })
                .toList();

        long aprobadas = contar(facturas, EstadoDte.APROBADO);
        long rechazadas = contar(facturas, EstadoDte.RECHAZADO);
        BigDecimal montoAprobado = facturas.stream()
                .filter(f -> f.getEstadoDte() == EstadoDte.APROBADO)
                .map(FacturaElectronica::getTotalGeneral)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant ultima = facturas.stream().map(FacturaElectronica::getFechaEmision).max(Comparator.naturalOrder()).orElse(null);

        return new HistorialClienteResponse(
                cliente.getRuc(),
                cliente.getRazonSocial(),
                facturas.size(),
                aprobadas,
                rechazadas,
                facturas.size() - aprobadas - rechazadas - contar(facturas, EstadoDte.ANULADO),
                montoAprobado,
                intentos.size(),
                intentos.stream().filter(i -> !i.isAprobado()).count(),
                ultima,
                detalle);
    }

    private static long contar(List<FacturaElectronica> facturas, EstadoDte estado) {
        return facturas.stream().filter(f -> f.getEstadoDte() == estado).count();
    }
}
