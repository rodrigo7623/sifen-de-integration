package com.tottalstore.sifen.registro;

import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.CondicionPago;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

/**
 * Archivo para la opción "Importar" del Registro de Comprobantes de Marangatu (RG 90/2021, obligación
 * 955), según la "Especificación Técnica para Importación Registro de Comprobantes" de la SET (v1,
 * junio 2021):
 *
 * <ul>
 *   <li>TXT delimitado por tabulaciones, UTF-8, sin fila de títulos (se eligió TXT y no CSV para que
 *       una coma en la razón social no corra las columnas).
 *   <li>Nombre {@code <RUC sin DV>_REG_MMAAAA_XXXXX}, comprimido en un ZIP con el mismo nombre; XXXXX
 *       cambia en cada archivo.
 *   <li>Hasta 5.000 filas por archivo: si hay más, se arman varios y se entregan juntos en un ZIP.
 *   <li>No se incluyen los comprobantes electrónicos (los DE propios ni los DE recibidos con CDC):
 *       Marangatu los toma del SIFEN.
 *   <li>Montos enteros, positivos y sin separador de miles; las notas de crédito también van en positivo.
 * </ul>
 */
@Component
public class ExportadorRg90 {

    static final int MAXIMO_FILAS = 5000;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMyyyy");

    private final ComprobanteRegistradoRepository repository;
    private final ConfiguracionEmisorService configuracionEmisorService;

    public ExportadorRg90(
            ComprobanteRegistradoRepository repository, ConfiguracionEmisorService configuracionEmisorService) {
        this.repository = repository;
        this.configuracionEmisorService = configuracionEmisorService;
    }

    public record Archivo(String nombre, byte[] contenido, int comprobantes) {
    }

    public Archivo exportar(YearMonth periodo) {
        ConfiguracionEmisor emisor = configuracionEmisorService.obtenerOCrear();
        String ruc = emisor.getRucBase();
        if (ruc == null || ruc.isBlank()) {
            throw new BusinessException("Cargá el RUC del emisor en Configuración: va en el nombre del archivo");
        }

        List<ComprobanteRegistrado> comprobantes = new ArrayList<>();
        for (TipoRegistro tipo : TipoRegistro.values()) {
            repository.delPeriodo(tipo, periodo.atDay(1), periodo.plusMonths(1).atDay(1)).stream()
                    .filter(c -> !c.esElectronico())
                    .forEach(comprobantes::add);
        }
        if (comprobantes.isEmpty()) {
            throw new BusinessException("No hay comprobantes para importar en " + MES.format(periodo)
                    + ": los electrónicos ya los tiene la SET y no van en el archivo");
        }

        List<String> filas = comprobantes.stream().map(ExportadorRg90::fila).toList();
        List<Archivo> lotes = new ArrayList<>();
        for (int desde = 0; desde < filas.size(); desde += MAXIMO_FILAS) {
            List<String> lote = filas.subList(desde, Math.min(desde + MAXIMO_FILAS, filas.size()));
            String nombre = ruc.trim() + "_REG_" + MES.format(periodo) + "_" + identificador();
            byte[] txt = (String.join("\r\n", lote) + "\r\n").getBytes(StandardCharsets.UTF_8);
            lotes.add(new Archivo(nombre + ".zip", zip(nombre + ".txt", txt), lote.size()));
        }
        if (lotes.size() == 1) {
            return lotes.get(0);
        }
        // Más de 5.000: Marangatu los importa de a uno; se entregan todos los ZIP dentro de otro.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Archivo lote : lotes) {
                zip.putNextEntry(new ZipEntry(lote.nombre()));
                zip.write(lote.contenido());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Archivo(ruc.trim() + "_REG_" + MES.format(periodo) + "_lotes.zip", out.toByteArray(), filas.size());
    }

    /** XXXXX del nombre: "T" + 4 dígitos de una secuencia, distinto en cada archivo generado. */
    private String identificador() {
        return String.format("T%04d", repository.siguienteNumeroExportacion() % 10000);
    }

    static String fila(ComprobanteRegistrado c) {
        boolean compra = c.getTipoRegistro() == TipoRegistro.COMPRA;
        List<String> campos = new ArrayList<>();
        campos.add(String.valueOf(c.getTipoRegistro().codigo()));
        campos.add(String.valueOf(c.getTipoIdentificacion().codigo()));
        campos.add(c.identificacionSinDv());
        campos.add(c.getRazonSocial() != null ? c.getRazonSocial() : "");
        campos.add(String.valueOf(c.getTipoComprobante().codigo()));
        campos.add(FECHA.format(c.getFechaEmision()));
        campos.add(c.getTimbrado());
        campos.add(c.getNumero() != null ? c.getNumero() : "");
        campos.add(entero(c.getGravado10()));
        campos.add(entero(c.getGravado5()));
        campos.add(entero(c.getExento()));
        campos.add(entero(c.getTotal()));
        campos.add(c.getCondicion() == CondicionPago.CREDITO ? "2" : "1");
        campos.add(sn(c.isMonedaExtranjera()));
        campos.add(sn(c.isImputaIva()));
        campos.add(sn(c.isImputaIre()));
        campos.add(sn(c.isImputaIrp()));
        if (compra) {
            // "NO IMPUTA" existe solo en el registro de compras (campo 18).
            campos.add(sn(c.isNoImputa()));
        }
        campos.add(c.getAsociadoNumero() != null ? c.getAsociadoNumero() : "");
        campos.add(c.getAsociadoTimbrado() != null ? c.getAsociadoTimbrado() : "");
        return String.join("\t", campos.stream().map(ExportadorRg90::limpiar).toList());
    }

    private static String entero(BigDecimal monto) {
        return monto.abs().setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String sn(boolean valor) {
        return valor ? "S" : "N";
    }

    /** Un tabulador o salto de línea dentro de un campo correría las columnas. */
    private static String limpiar(String valor) {
        return valor.replaceAll("[\\t\\r\\n]+", " ").trim();
    }

    private static byte[] zip(String nombreEntrada, byte[] contenido) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(nombreEntrada));
            zip.write(contenido);
            zip.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}
