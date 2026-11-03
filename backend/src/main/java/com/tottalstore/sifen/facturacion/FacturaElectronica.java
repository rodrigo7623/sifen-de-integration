package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Documento electrónico emitido: la factura cargada manualmente (RF-11 a RF-17, CU-02) o una nota de
 * crédito/débito que la ajusta (M6). Todos comparten el mismo ciclo de vida del DTE; el tipo
 * distingue qué grupos lleva el XML.
 */
@Entity
@Table(name = "factura_electronica")
@Getter
@Setter
@NoArgsConstructor
public class FacturaElectronica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_factura")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_doc")
    private TipoDocumento tipoDoc = TipoDocumento.FE;

    /** Solo en notas: la factura que ajusta (H004 dCdCDERef = su CDC). EAGER por el mismo motivo que
     * "cliente": FacturaResponse.from() muestra su número fuera de la transacción. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "factura_asociada_id")
    private FacturaElectronica facturaAsociada;

    /** Solo en notas: E401 iMotEmi. */
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_emision")
    private MotivoNota motivoEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_dte")
    private EstadoDte estadoDte = EstadoDte.BORRADOR;

    @Column(name = "total_iva5")
    private BigDecimal totalIva5 = BigDecimal.ZERO;

    @Column(name = "total_iva10")
    private BigDecimal totalIva10 = BigDecimal.ZERO;

    @Column(name = "total_general")
    private BigDecimal totalGeneral = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_pago")
    private CondicionPago condicionPago;

    @Column(name = "plazo_dias")
    private Integer plazoDias;

    @Column(name = "cantidad_cuotas")
    private Integer cantidadCuotas;

    @Column(name = "fecha_emision")
    private Instant fechaEmision = Instant.now();

    @Column(name = "email_enviado")
    private boolean emailEnviado = false;

    /** Número del documento dentro de su punto de expedición (C007 dNumDoc). Se asigna al confirmar
     * por primera vez y se conserva en los reenvíos tras un rechazo. */
    @Column(name = "numero_documento")
    private Integer numeroDocumento;

    /** CDC (A002) con el que se firmó y envió el DE por última vez. */
    private String cdc;

    /** Código de seguridad (B004 dCodSeg) que forma parte del CDC. */
    @Column(name = "codigo_seguridad")
    private String codigoSeguridad;

    // EAGER a propósito: FacturaResponse.from() siempre necesita razón social del cliente al leer
    // una factura, y open-in-view=false hace que un proxy LAZY explote (LazyInitializationException)
    // fuera del método @Transactional que cargó la factura. Es un @ManyToOne, no un @OneToMany: no
    // genera problema de N+1, Hibernate lo resuelve con un JOIN.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_ruc", referencedColumnName = "ruc")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // EAGER por el mismo motivo que "cliente": FacturaResponse.from() lee el establecimiento/punto
    // de expedición fuera de la transacción que cargó la factura.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "establecimiento_id")
    private Establecimiento establecimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "punto_expedicion_id")
    private PuntoExpedicion puntoExpedicion;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemFactura> items = new ArrayList<>();

    // EAGER por el mismo motivo que "cliente": FacturaResponse.from() lee el motivo de rechazo y el
    // XML firmado fuera de la transacción que cargó la factura; es un OneToOne, sin riesgo de N+1.
    @OneToOne(mappedBy = "factura", fetch = FetchType.EAGER)
    private RespuestaSifen respuestaSifen;

    /** Número impreso del comprobante (establecimiento-punto-número, p. ej. 001-001-0000001), o
     * null si todavía no se le asignó número (borrador nunca confirmado). */
    public String numeroComprobante() {
        if (numeroDocumento == null || establecimiento == null || puntoExpedicion == null) {
            return null;
        }
        return establecimiento.getCodigo() + "-" + puntoExpedicion.getCodigo() + "-"
                + String.format("%07d", numeroDocumento);
    }

    /** CDC con el que el SIFEN conoce al DE: el del DE firmado o, en modo stub (que no firma), el
     * simulado que devuelve el envío. */
    public String cdcVigente() {
        if (cdc != null) {
            return cdc;
        }
        return respuestaSifen != null ? respuestaSifen.getCdc() : null;
    }

    public boolean esNota() {
        return tipoDoc != null && tipoDoc.esNota();
    }
}
