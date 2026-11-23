package com.tottalstore.sifen.establecimientos;

import java.util.Optional;
import java.util.UUID;

/** Fragmento de {@link PuntoExpedicionRepository} para tomar el punto de expedición al numerar. */
public interface PuntoExpedicionNumerador {

    /**
     * Bloquea la fila del punto de expedición (SELECT ... FOR UPDATE) y devuelve sus valores
     * <b>releídos de la base</b>, no los que ya estaban en memoria.
     *
     * <p>BUG-025: la factura carga su punto de expedición de forma EAGER, así que una consulta con
     * bloqueo devolvía la instancia que Hibernate ya tenía en el contexto de persistencia, con el
     * último número leído antes de obtener el bloqueo. Dos confirmaciones simultáneas calculaban
     * entonces el mismo número y una terminaba en la restricción única idx_factura_numeracion.
     */
    Optional<PuntoExpedicion> bloquearParaNumerar(UUID id);
}
