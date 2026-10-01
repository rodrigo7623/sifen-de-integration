package com.tottalstore.sifen.establecimientos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Establecimiento (local/sucursal) desde el que se pueden emitir facturas -- grupo D1 del Manual
 * Técnico del SIFEN. Cada establecimiento tiene uno o más {@link PuntoExpedicion}.
 */
@Entity
@Table(name = "establecimiento")
@Getter
@Setter
@NoArgsConstructor
public class Establecimiento {

    @Id
    private UUID id;

    private String codigo;

    private String denominacion;

    private String direccion;

    @Column(name = "numero_casa")
    private String numeroCasa;

    @Column(name = "departamento_codigo")
    private String departamentoCodigo;

    @Column(name = "departamento_descripcion")
    private String departamentoDescripcion;

    @Column(name = "distrito_codigo")
    private String distritoCodigo;

    @Column(name = "distrito_descripcion")
    private String distritoDescripcion;

    @Column(name = "ciudad_codigo")
    private String ciudadCodigo;

    @Column(name = "ciudad_descripcion")
    private String ciudadDescripcion;

    private String telefono;

    private String email;

    private boolean activo = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // EAGER a propósito: EstablecimientoResponse.from() siempre lista los puntos de expedición
    // junto con el establecimiento; evita el patrón de LazyInitializationException ya visto (A2/C1).
    @OneToMany(mappedBy = "establecimiento", fetch = FetchType.EAGER)
    private List<PuntoExpedicion> puntosExpedicion = new ArrayList<>();
}
