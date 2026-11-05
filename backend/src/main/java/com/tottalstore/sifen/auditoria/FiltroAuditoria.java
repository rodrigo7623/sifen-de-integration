package com.tottalstore.sifen.auditoria;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.jpa.domain.Specification;

/** Filtros del visor de auditoría. Las fechas son días de Paraguay ({@code hasta} inclusive). */
public record FiltroAuditoria(
        UUID facturaId,
        LocalDate desde,
        LocalDate hasta,
        String operacion,
        UUID usuarioId,
        TipoDocumento tipoDocumento,
        String q) {

    static final ZoneId ZONA_PARAGUAY = ZoneId.of("America/Asuncion");

    /** 001-001-0000005: establecimiento, punto de expedición y número. */
    private static final Pattern NUMERO_COMPLETO = Pattern.compile("(\\d{3})-(\\d{3})-(\\d{1,7})");

    public Specification<LogAuditoria> especificacion() {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaHora"), desde.atStartOfDay(ZONA_PARAGUAY).toInstant()));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThan(root.get("fechaHora"), hasta.plusDays(1).atStartOfDay(ZONA_PARAGUAY).toInstant()));
            }
            if (operacion != null && !operacion.isBlank()) {
                condiciones.add(cb.equal(root.get("operacion"), operacion.trim()));
            }
            if (usuarioId != null) {
                condiciones.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }
            boolean filtraDocumento = facturaId != null || tipoDocumento != null || (q != null && !q.isBlank());
            if (filtraDocumento) {
                Join<LogAuditoria, FacturaElectronica> factura = root.join("factura", JoinType.INNER);
                if (facturaId != null) {
                    condiciones.add(cb.equal(factura.get("id"), facturaId));
                }
                if (tipoDocumento != null) {
                    condiciones.add(cb.equal(factura.get("tipoDoc"), tipoDocumento));
                }
                if (q != null && !q.isBlank()) {
                    condiciones.add(coincideTexto(q.trim(), factura, cb));
                }
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static Predicate coincideTexto(String texto, Join<LogAuditoria, FacturaElectronica> factura,
                                           jakarta.persistence.criteria.CriteriaBuilder cb) {
        var cliente = factura.join("cliente", JoinType.LEFT);
        String patron = "%" + texto.toLowerCase() + "%";
        List<Predicate> alternativas = new ArrayList<>();
        alternativas.add(cb.like(cb.lower(cliente.get("ruc")), patron));
        alternativas.add(cb.like(cb.lower(cliente.get("razonSocial")), patron));

        Matcher completo = NUMERO_COMPLETO.matcher(texto);
        if (completo.matches()) {
            alternativas.add(cb.and(
                    cb.equal(factura.get("establecimiento").get("codigo"), completo.group(1)),
                    cb.equal(factura.get("puntoExpedicion").get("codigo"), completo.group(2)),
                    cb.equal(factura.get("numeroDocumento"), Integer.parseInt(completo.group(3)))));
        } else if (texto.matches("\\d{1,7}")) {
            alternativas.add(cb.equal(factura.get("numeroDocumento"), Integer.parseInt(texto)));
        }
        return cb.or(alternativas.toArray(Predicate[]::new));
    }
}
