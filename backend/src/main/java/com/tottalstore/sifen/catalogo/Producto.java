package com.tottalstore.sifen.catalogo;

import com.tottalstore.sifen.shared.TasaIva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Producto del catálogo interno (RF-14). */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    private String codigo;

    private String descripcion;

    @Column(name = "unidad_medida")
    private String unidadMedida;

    @Column(name = "precio_base")
    private BigDecimal precioBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "tasa_iva")
    private TasaIva tasaIva;

    private boolean activo = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
