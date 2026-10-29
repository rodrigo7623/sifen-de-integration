package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.reportes.dto.HistorialClienteResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.ReporteRechazosResponse;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reportes del Release 3: Libro IVA Ventas (M4), estadísticas de rechazo e historial por cliente. */
@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final LibroIvaService libroIvaService;
    private final LibroIvaExcelExporter excelExporter;
    private final LibroIvaPdfExporter pdfExporter;
    private final ReportesService reportesService;

    public ReportesController(
            LibroIvaService libroIvaService,
            LibroIvaExcelExporter excelExporter,
            LibroIvaPdfExporter pdfExporter,
            ReportesService reportesService) {
        this.libroIvaService = libroIvaService;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
        this.reportesService = reportesService;
    }

    /** @param periodo mes en formato {@code yyyy-MM}, por ejemplo {@code 2026-10}. */
    @GetMapping("/libro-iva")
    public LibroIvaResponse libroIva(@RequestParam YearMonth periodo) {
        return libroIvaService.generar(periodo);
    }

    @GetMapping("/libro-iva/excel")
    public ResponseEntity<byte[]> libroIvaExcel(@RequestParam YearMonth periodo) {
        return archivo(excelExporter.exportar(libroIvaService.generar(periodo)), XLSX, "libro-iva-ventas-" + periodo + ".xlsx");
    }

    @GetMapping("/libro-iva/pdf")
    public ResponseEntity<byte[]> libroIvaPdf(@RequestParam YearMonth periodo) {
        return archivo(pdfExporter.exportar(libroIvaService.generar(periodo)), MediaType.APPLICATION_PDF,
                "libro-iva-ventas-" + periodo + ".pdf");
    }

    @GetMapping("/rechazos")
    public ReporteRechazosResponse rechazos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportesService.rechazos(desde, hasta);
    }

    @GetMapping("/clientes/{ruc}")
    public HistorialClienteResponse historialCliente(@PathVariable String ruc) {
        return reportesService.historialCliente(ruc);
    }

    private ResponseEntity<byte[]> archivo(byte[] contenido, MediaType tipo, String nombre) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header("Content-Disposition", "attachment; filename=\"" + nombre + "\"")
                .body(contenido);
    }
}
