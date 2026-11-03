package com.tottalstore.sifen.establecimientos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Punto de expedición (caja/terminal) dentro de un {@link Establecimiento} -- D017 del manual. */
@Entity
@Table(name = "punto_expedicion")
@Getter
@Setter
@NoArgsConstructor
public class PuntoExpedicion {

    @Id
    private UUID id;

    // EAGER por el mismo motivo que Establecimiento.puntosExpedicion: evitar el patrón de
    // LazyInitializationException ya documentado dos veces en este proyecto (A2/C1).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "establecimiento_id")
    private Establecimiento establecimiento;

    private String codigo;

    private String descripcion;

    private boolean activo = true;

    /** Último número de factura (C007 dNumDoc) asignado en este punto de expedición. */
    @Column(name = "ultimo_numero_documento")
    private int ultimoNumeroDocumento;

    /** Últimos números de nota de crédito y de débito: cada tipo de DE se numera por separado. */
    @Column(name = "ultimo_numero_nc")
    private int ultimoNumeroNc;

    @Column(name = "ultimo_numero_nd")
    private int ultimoNumeroNd;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
