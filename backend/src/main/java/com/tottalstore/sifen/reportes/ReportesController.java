package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.reportes.dto.HistorialClienteResponse;
import com.tottalstore.sifen.registro.ExportadorRg90;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.TipoLibro;
import com.tottalstore.sifen.reportes.dto.LiquidacionIvaResponse;
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

/** Reportes del Release 3: Libros IVA Ventas y Compras (M4), liquidación, archivo RG 90, rechazos e historial por cliente. */
@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final LibroIvaService libroIvaService;
    private final LibroIvaExcelExporter excelExporter;
    private final LibroIvaPdfExporter pdfExporter;
    private final ReportesService reportesService;
    private final ExportadorRg90 exportadorRg90;

    public ReportesController(
            LibroIvaService libroIvaService,
            LibroIvaExcelExporter excelExporter,
            LibroIvaPdfExporter pdfExporter,
            ReportesService reportesService,
            ExportadorRg90 exportadorRg90) {
        this.libroIvaService = libroIvaService;
        this.excelExporter = excelExporter;
        this.pdfExporter = pdfExporter;
        this.reportesService = reportesService;
        this.exportadorRg90 = exportadorRg90;
    }

    /** @param periodo mes en formato {@code yyyy-MM}, por ejemplo {@code 2026-10}. */
    @GetMapping("/libro-iva")
    public LibroIvaResponse libroIva(
            @RequestParam YearMonth periodo, @RequestParam(defaultValue = "VENTAS") TipoLibro libro) {
        return libroIvaService.generar(periodo, libro);
    }

    @GetMapping("/libro-iva/excel")
    public ResponseEntity<byte[]> libroIvaExcel(
            @RequestParam YearMonth periodo, @RequestParam(defaultValue = "VENTAS") TipoLibro libro) {
        return archivo(excelExporter.exportar(libroIvaService.generar(periodo, libro)), XLSX,
                libro.archivo() + "-" + periodo + ".xlsx");
    }

    @GetMapping("/libro-iva/pdf")
    public ResponseEntity<byte[]> libroIvaPdf(
            @RequestParam YearMonth periodo, @RequestParam(defaultValue = "VENTAS") TipoLibro libro) {
        return archivo(pdfExporter.exportar(libroIvaService.generar(periodo, libro)), MediaType.APPLICATION_PDF,
                libro.archivo() + "-" + periodo + ".pdf");
    }

    /** Débito fiscal menos crédito fiscal del mes (referencial para el Formulario 120). */
    @GetMapping("/liquidacion-iva")
    public LiquidacionIvaResponse liquidacionIva(@RequestParam YearMonth periodo) {
        return libroIvaService.liquidacion(periodo);
    }

    /** ZIP para importar en Marangatu (RG 90/2021): compras y ventas no electrónicas del mes. */
    @GetMapping("/registro-rg90")
    public ResponseEntity<byte[]> registroRg90(@RequestParam YearMonth periodo) {
        ExportadorRg90.Archivo archivo = exportadorRg90.exportar(periodo);
        return archivo(archivo.contenido(), MediaType.parseMediaType("application/zip"), archivo.nombre());
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
