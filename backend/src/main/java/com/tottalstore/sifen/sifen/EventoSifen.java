package com.tottalstore.sifen.sifen;

import com.tottalstore.sifen.auth.Usuario;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento del emisor enviado al SIFEN por {@code siRecepEvento} (Manual Técnico v150, 9.5 y 11.5.1):
 * cancelación de un DTE aprobado o inutilización del número de un DE que el SIFEN rechazó. El
 * {@code id} es el atributo {@code Id} del {@code <rEve>} firmado (GDE003).
 */
@Entity
@Table(name = "evento_sifen")
@Getter
@Setter
@NoArgsConstructor
public class EventoSifen {

    public enum Tipo { CANCELACION, INUTILIZACION }

    public enum Estado { PENDIENTE, APROBADO, RECHAZADO, SIN_CONFIRMAR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @Enumerated(EnumType.STRING)
    private Tipo tipo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "factura_id")
    private FacturaElectronica factura;

    private String motivo;

    private Instant fecha = Instant.now();

    @Enumerated(EnumType.STRING)
    private Ambiente ambiente;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.PENDIENTE;

    private String codigo;

    private String mensaje;

    /** dProtAut: número de transacción que el SIFEN asigna al evento aprobado. */
    private String protocolo;

    @Column(name = "xml_firmado")
    private String xmlFirmado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
