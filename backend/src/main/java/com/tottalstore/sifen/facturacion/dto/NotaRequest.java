package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.MotivoNota;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Nota de crédito/débito sobre una factura aprobada (M6). El cliente, el establecimiento y el punto
 * de expedición se toman de la factura asociada; al editar un borrador, {@code facturaAsociadaId} y
 * {@code tipo} deben coincidir con los de la nota.
 */
public record NotaRequest(
        @NotNull(message = "La factura asociada es obligatoria") UUID facturaAsociadaId,
        @NotNull(message = "El tipo de nota es obligatorio") TipoDocumento tipo,
        @NotNull(message = "El motivo de emisión es obligatorio") MotivoNota motivo,
        @NotEmpty(message = "La nota debe tener al menos un ítem") @Valid List<ItemFacturaRequest> items) {
}
