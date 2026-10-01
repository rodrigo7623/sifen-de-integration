package com.tottalstore.sifen.sifen;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Registro histórico, append-only, de cada intento real de conexión al SIFEN — incluidos los que
 * fallan antes de recibir respuesta (TLS, red, timeout). A diferencia de {@code AuditoriaService}
 * (que audita operaciones dentro de la app), esto es evidencia de la integración real con el
 * SIFEN a lo largo del tiempo: pedido explícito del usuario para poder mostrar cómo fue avanzando
 * la integración, no solo el resultado del último intento.
 */
@Component
public class SifenEnvioHistorial {

    private static final Logger log = LoggerFactory.getLogger(SifenEnvioHistorial.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path archivo;

    public SifenEnvioHistorial(@Value("${sifen.envio.historial-path:./sifen-envios-historial.jsonl}") String ruta) {
        this.archivo = Path.of(ruta);
    }

    public void registrar(
            UUID facturaId,
            Ambiente ambiente,
            String urlEndpoint,
            String dId,
            String resultado,
            String codigo,
            String mensaje,
            String cdc) {
        Map<String, Object> linea = new LinkedHashMap<>();
        linea.put("timestamp", Instant.now().toString());
        linea.put("facturaId", facturaId != null ? facturaId.toString() : null);
        linea.put("ambiente", ambiente != null ? ambiente.toString() : null);
        linea.put("urlEndpoint", urlEndpoint);
        linea.put("dId", dId);
        linea.put("resultado", resultado);
        linea.put("codigo", codigo);
        linea.put("mensaje", mensaje);
        linea.put("cdc", cdc);

        try {
            Files.writeString(
                    archivo,
                    MAPPER.writeValueAsString(linea) + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            // No queremos que un problema de disco tumbe el flujo de facturación por no poder
            // escribir el historial — se loguea y se sigue.
            log.warn("No se pudo escribir en el historial de envíos SIFEN ({})", archivo, e);
        }
    }
}
