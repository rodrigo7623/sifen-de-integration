package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.clientes.Cliente;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Factura electrónica cargada manualmente (RF-11 a RF-17, CU-02). */
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

    @Column(name = "tipo_doc")
    private String tipoDoc = "FE";

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

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ItemFactura> items = new ArrayList<>();
}
