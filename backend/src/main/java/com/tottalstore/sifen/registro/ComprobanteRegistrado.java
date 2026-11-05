package com.tottalstore.sifen.registro;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.facturacion.CondicionPago;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comprobante de compra o de venta no electrónica, con los campos del Registro de Comprobantes de la
 * RG 90/2021. Los montos gravados incluyen el IVA (igual que en el archivo de Marangatu).
 */
@Entity
@Table(name = "comprobante_registrado")
@Getter
@Setter
@NoArgsConstructor
public class ComprobanteRegistrado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_registro")
    private TipoRegistro tipoRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante")
    private TipoComprobanteRg90 tipoComprobante;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_identificacion")
    private TipoIdentificacion tipoIdentificacion;

    private String identificacion;

    @Column(name = "razon_social")
    private String razonSocial;

    private String timbrado;

    private String numero;

    /** Solo compras: CDC del DE recibido del proveedor. Si está, el comprobante no va al archivo RG 90. */
    private String cdc;

    @Enumerated(EnumType.STRING)
    private CondicionPago condicion = CondicionPago.CONTADO;

    @Column(name = "moneda_extranjera")
    private boolean monedaExtranjera;

    private BigDecimal gravado10 = BigDecimal.ZERO;
    private BigDecimal gravado5 = BigDecimal.ZERO;
    private BigDecimal exento = BigDecimal.ZERO;
    private BigDecimal total;
    private BigDecimal iva10 = BigDecimal.ZERO;
    private BigDecimal iva5 = BigDecimal.ZERO;

    @Column(name = "imputa_iva")
    private boolean imputaIva = true;

    @Column(name = "imputa_ire")
    private boolean imputaIre;

    @Column(name = "imputa_irp")
    private boolean imputaIrp;

    @Column(name = "no_imputa")
    private boolean noImputa;

    @Column(name = "asociado_numero")
    private String asociadoNumero;

    @Column(name = "asociado_timbrado")
    private String asociadoTimbrado;

    private String observacion;

    // EAGER: la respuesta se arma fuera de la transacción (open-in-view=false).
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modificado_por")
    private Usuario modificadoPor;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    /** Un DE recibido (con CDC) ya lo tiene la SET: se excluye del archivo de importación. */
    public boolean esElectronico() {
        return cdc != null && !cdc.isBlank();
    }

    /** Identificación sin el dígito verificador, como la pide el archivo de Marangatu. */
    public String identificacionSinDv() {
        if (tipoIdentificacion == TipoIdentificacion.RUC && identificacion != null && identificacion.contains("-")) {
            return identificacion.substring(0, identificacion.indexOf('-'));
        }
        return identificacion;
    }
}
