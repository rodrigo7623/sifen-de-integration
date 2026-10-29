package com.tottalstore.sifen.sifen;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/**
 * Un intento de envío de una factura al SIFEN y su resultado. A diferencia de {@link RespuestaSifen}
 * (que guarda solo la última respuesta), acá queda cada intento: es la base de las estadísticas de
 * rechazo del Release 3.
 */
@Entity
@Table(name = "intento_envio_sifen")
@Getter
@Setter
@NoArgsConstructor
public class IntentoEnvioSifen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_intento")
    private UUID id;

    // EAGER: los reportes leen el cliente de la factura fuera de la transacción (open-in-view=false).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "factura_id")
    private FacturaElectronica factura;

    private Instant fecha = Instant.now();

    @Enumerated(EnumType.STRING)
    private Ambiente ambiente;

    private boolean aprobado;

    private String codigo;

    private String descripcion;
}
