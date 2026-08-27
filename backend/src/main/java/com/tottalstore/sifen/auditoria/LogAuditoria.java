package com.tottalstore.sifen.auditoria;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro de auditoría de operaciones sobre facturas (RF-08). */
@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_log")
    private UUID id;

    private String operacion;

    @Column(name = "fecha_hora")
    private Instant fechaHora = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private FacturaElectronica factura;
}
