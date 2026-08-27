package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.catalogo.Producto;
import com.tottalstore.sifen.shared.TasaIva;
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
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ítem (línea) de una factura manual: desde catálogo (RF-12) o ad-hoc (RF-13). */
@Entity
@Table(name = "item_factura")
@Getter
@Setter
@NoArgsConstructor
public class ItemFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_item")
    private UUID id;

    private String descripcion;

    private Integer cantidad;

    @Column(name = "precio_unitario")
    private BigDecimal precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tasa_iva")
    private TasaIva tasaIva;

    /** Cantidad × precio unitario, IVA incluido (convención SIFEN). */
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private FacturaElectronica factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_codigo")
    private Producto producto;
}
