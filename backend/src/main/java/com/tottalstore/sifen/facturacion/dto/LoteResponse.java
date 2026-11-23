package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import com.tottalstore.sifen.sifen.LoteSifen;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoteResponse(
        UUID id,
        TipoDocumento tipoDocumento,
        /** dProtConsLote. */
        String numeroLote,
        LoteSifen.Estado estado,
        String codigo,
        String mensaje,
        Instant fechaEnvio,
        Instant fechaConsulta,
        int cantidad,
        List<Documento> documentos) {

    public record Documento(UUID id, String numero, EstadoDte estadoDte, String cdc, String mensaje) {
    }

    public static LoteResponse from(LoteSifen l, List<FacturaElectronica> documentos) {
        return new LoteResponse(l.getId(), l.getTipoDoc(), l.getNumeroLote(), l.getEstado(), l.getCodigo(), l.getMensaje(),
                l.getFechaEnvio(), l.getFechaConsulta(), l.getCantidad(),
                documentos.stream().map(f -> new Documento(f.getId(), f.numeroComprobante(), f.getEstadoDte(), f.cdcVigente(),
                        f.getRespuestaSifen() != null ? f.getRespuestaSifen().getDescripcion() : null)).toList());
    }
}
