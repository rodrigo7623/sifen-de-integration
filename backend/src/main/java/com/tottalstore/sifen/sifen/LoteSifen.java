package com.tottalstore.sifen.sifen;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.facturacion.TipoDocumento;
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
 * Lote de hasta 50 DE del mismo tipo enviado por el WS asíncrono {@code siRecepLoteDE} (Manual
 * Técnico v150, 9.2). El SIFEN devuelve un número de lote ({@code dProtConsLote}) con el que después
 * se consulta el resultado de cada DE ({@code siResultLoteDE}, 9.3).
 */
@Entity
@Table(name = "lote_sifen")
@Getter
@Setter
@NoArgsConstructor
public class LoteSifen {

    /**
     * RECIBIDO: el SIFEN lo encoló (0300) y hay que consultar el resultado. NO_RECIBIDO: lo rechazó en
     * las validaciones iniciales; sus DE vuelven a FIRMADO. SIN_CONFIRMAR: no hubo respuesta; sus DE
     * quedan EN_PROCESO para verificarlos por CDC. CONCLUIDO: el SIFEN terminó de procesarlo (0362).
     */
    public enum Estado { RECIBIDO, NO_RECIBIDO, SIN_CONFIRMAR, CONCLUIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_lote")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_doc")
    private TipoDocumento tipoDoc;

    /** dProtConsLote. */
    @Column(name = "numero_lote")
    private String numeroLote;

    @Enumerated(EnumType.STRING)
    private Estado estado;

    private String codigo;

    private String mensaje;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio = Instant.now();

    @Column(name = "fecha_consulta")
    private Instant fechaConsulta;

    private int cantidad;

    @Enumerated(EnumType.STRING)
    private Ambiente ambiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
