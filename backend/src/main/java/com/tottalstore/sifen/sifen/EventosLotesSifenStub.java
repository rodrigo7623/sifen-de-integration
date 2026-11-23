package com.tottalstore.sifen.sifen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación stub (default) de eventos y lotes: aprueba los eventos y procesa los lotes en
 * memoria. Igual que {@link EnviadorSifenStub}, un DE con la marca "RECHAZAR" sale rechazado. Como el
 * DE simulado no tiene CDC, cada resultado se identifica con el {@code <idFactura>} del XML.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "stub", matchIfMissing = true)
public class EventosLotesSifenStub implements EventosLotesSifenService {

    private static final Pattern ID_FACTURA = Pattern.compile("<idFactura>([^<]+)</idFactura>");
    private final AtomicLong secuencia = new AtomicLong(System.currentTimeMillis() % 1_000_000_000L);
    private final Map<String, List<String>> lotes = new ConcurrentHashMap<>();

    @Override
    public RespuestaEvento enviarEvento(long idEvento, String gGroupGesEveFirmado, Ambiente ambiente) {
        return new RespuestaEvento(true, "0600", "Aprobado — Evento registrado correctamente (SIMULADO, ambiente "
                + ambiente + ")", String.valueOf(secuencia.incrementAndGet()), false);
    }

    @Override
    public RespuestaLote enviarLote(List<String> rDeFirmados, Ambiente ambiente) {
        String numero = String.valueOf(secuencia.incrementAndGet());
        lotes.put(numero, List.copyOf(rDeFirmados));
        return new RespuestaLote(true, "0300", "Lote recibido con éxito (SIMULADO)", numero, false);
    }

    @Override
    public ResultadoLote consultarLote(String numeroLote, Ambiente ambiente) {
        List<String> des = lotes.get(numeroLote);
        if (des == null) {
            return new ResultadoLote("0360", "Número de lote inexistente (SIMULADO)", List.of(), false);
        }
        List<ResultadoDe> resultados = new ArrayList<>();
        for (String xml : des) {
            Matcher m = ID_FACTURA.matcher(xml);
            String id = m.find() ? m.group(1) : null;
            boolean rechazo = xml.toUpperCase().contains("RECHAZAR");
            resultados.add(rechazo
                    ? new ResultadoDe(id, false, "Rechazado", "0160", "Rechazado — XML con la marca RECHAZAR (SIMULADO)")
                    : new ResultadoDe(id, true, "Aprobado", "0260", "Aprobado — Autorización del DE satisfactoria (SIMULADO)"));
        }
        return new ResultadoLote(ResultadoLote.CONCLUIDO, "Procesamiento de lote concluido (SIMULADO)", resultados, false);
    }
}
