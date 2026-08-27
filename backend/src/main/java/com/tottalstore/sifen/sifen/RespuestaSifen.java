package com.tottalstore.sifen.sifen;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "respuesta_sifen")
@Getter
@Setter
@NoArgsConstructor
public class RespuestaSifen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_respuesta")
    private UUID id;

    private String codigo;

    private String descripcion;

    private String cdc;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta = Instant.now();

    @OneToOne
    @JoinColumn(name = "factura_id", unique = true)
    private FacturaElectronica factura;
}
