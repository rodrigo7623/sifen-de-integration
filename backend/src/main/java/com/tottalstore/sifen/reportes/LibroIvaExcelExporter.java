package com.tottalstore.sifen.reportes;

import com.tottalstore.sifen.reportes.dto.LibroIvaResponse;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Renglon;
import com.tottalstore.sifen.reportes.dto.LibroIvaResponse.Totales;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Exporta el Libro IVA Ventas a .xlsx (M4), con los montos como números para que se puedan sumar. */
@Component
public class LibroIvaExcelExporter {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static final String[] COLUMNAS = {
        "Fecha", "Tipo", "Número", "Timbrado", "RUC/CI cliente", "Razón social", "Condición",
        "Gravado 10% (IVA incl.)", "IVA 10%", "Gravado 5% (IVA incl.)", "IVA 5%", "Exento", "Total", "CDC"
    };

    private static final int[] ANCHOS = {12, 20, 18, 11, 16, 34, 11, 22, 14, 22, 14, 14, 16, 48};

    public byte[] exportar(LibroIvaResponse libro) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet hoja = wb.createSheet("Libro IVA Ventas " + libro.periodo());

            Font negrita = wb.createFont();
            negrita.setBold(true);
            CellStyle titulo = wb.createCellStyle();
            Font fuenteTitulo = wb.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            titulo.setFont(fuenteTitulo);

            CellStyle encabezado = wb.createCellStyle();
            encabezado.setFont(negrita);
            encabezado.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            encabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle monto = wb.createCellStyle();
            monto.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            CellStyle montoTotal = wb.createCellStyle();
            montoTotal.cloneStyleFrom(monto);
            montoTotal.setFont(negrita);
            CellStyle textoTotal = wb.createCellStyle();
            textoTotal.setFont(negrita);

            int fila = 0;
            Row r = hoja.createRow(fila++);
            r.createCell(0).setCellValue("Libro IVA Ventas — período " + libro.periodo());
            r.getCell(0).setCellStyle(titulo);
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
            r = hoja.createRow(fila++);
            r.createCell(0).setCellValue("Contribuyente: " + nvl(libro.emisorRazonSocial()) + " · RUC " + nvl(libro.emisorRuc()));
            fila++;

            r = hoja.createRow(fila++);
            for (int c = 0; c < COLUMNAS.length; c++) {
                r.createCell(c).setCellValue(COLUMNAS[c]);
                r.getCell(c).setCellStyle(encabezado);
            }

            for (Renglon x : libro.renglones()) {
                r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(FECHA.format(x.fecha()));
                r.createCell(1).setCellValue(x.tipoComprobante());
                r.createCell(2).setCellValue(x.numero());
                r.createCell(3).setCellValue(nvl(x.timbrado()));
                r.createCell(4).setCellValue(x.clienteRuc());
                r.createCell(5).setCellValue(x.clienteRazonSocial());
                r.createCell(6).setCellValue(x.condicion());
                numero(r, 7, x.gravado10(), monto);
                numero(r, 8, x.iva10(), monto);
                numero(r, 9, x.gravado5(), monto);
                numero(r, 10, x.iva5(), monto);
                numero(r, 11, x.exento(), monto);
                numero(r, 12, x.total(), monto);
                r.createCell(13).setCellValue(nvl(x.cdc()));
            }

            Totales t = libro.totales();
            r = hoja.createRow(fila++);
            r.createCell(0).setCellValue("Totales (" + t.cantidadComprobantes() + " comprobantes)");
            r.getCell(0).setCellStyle(textoTotal);
            numero(r, 7, t.gravado10(), montoTotal);
            numero(r, 8, t.iva10(), montoTotal);
            numero(r, 9, t.gravado5(), montoTotal);
            numero(r, 10, t.iva5(), montoTotal);
            numero(r, 11, t.exento(), montoTotal);
            numero(r, 12, t.total(), montoTotal);

            fila++;
            fila = resumen(hoja, fila, "Base imponible 10%", t.baseImponible10(), textoTotal, monto);
            fila = resumen(hoja, fila, "Base imponible 5%", t.baseImponible5(), textoTotal, monto);
            resumen(hoja, fila, "IVA débito fiscal total", t.totalIva(), textoTotal, montoTotal);

            // Anchos fijos: autoSizeColumn usa AWT y falla en contenedores sin fuentes (Render).
            for (int c = 0; c < COLUMNAS.length; c++) {
                hoja.setColumnWidth(c, ANCHOS[c] * 256);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el Excel del Libro IVA " + libro.periodo(), e);
        }
    }

    private int resumen(Sheet hoja, int fila, String etiqueta, BigDecimal valor, CellStyle estiloEtiqueta, CellStyle estiloValor) {
        Row r = hoja.createRow(fila);
        r.createCell(5).setCellValue(etiqueta);
        r.getCell(5).setCellStyle(estiloEtiqueta);
        numero(r, 8, valor, estiloValor);
        return fila + 1;
    }

    private void numero(Row r, int columna, BigDecimal valor, CellStyle estilo) {
        r.createCell(columna).setCellValue(valor.doubleValue());
        r.getCell(columna).setCellStyle(estilo);
    }

    private static String nvl(String valor) {
        return valor != null ? valor : "-";
    }
}
