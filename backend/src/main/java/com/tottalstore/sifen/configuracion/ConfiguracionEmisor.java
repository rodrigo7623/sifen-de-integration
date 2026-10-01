package com.tottalstore.sifen.configuracion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del emisor (razón social, dirección, actividad económica, timbrado) a cargo del admin
 * desde la UI, en vez de variables de entorno. Fila única (patrón "singleton row").
 *
 * <p>Todavía NO está conectada a {@code sifen.emisor.*} ({@code EmisorConfig}) ni al CDC/XML real
 * -- es almacenamiento preparatorio para cuando se retome la Fase 3 de la integración real al
 * SIFEN. Nombres de campo alineados a los tags del Manual Técnico v150 (grupo D2) para que ese
 * mapeo futuro sea directo.
 *
 * <p>El establecimiento/punto de expedición dejaron de vivir acá: ahora el sistema soporta
 * múltiples, en las entidades {@code Establecimiento}/{@code PuntoExpedicion} (paquete
 * {@code establecimientos}), elegibles por factura.
 */
@Entity
@Table(name = "configuracion_emisor")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionEmisor {

    @Id
    private UUID id;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "nombre_fantasia")
    private String nombreFantasia;

    private String direccion;

    @Column(name = "numero_casa")
    private String numeroCasa;

    @Column(name = "complemento_direccion1")
    private String complementoDireccion1;

    @Column(name = "complemento_direccion2")
    private String complementoDireccion2;

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

    @Column(name = "actividad_economica_codigo")
    private String actividadEconomicaCodigo;

    @Column(name = "actividad_economica_descripcion")
    private String actividadEconomicaDescripcion;

    @Column(name = "ruc_base")
    private String rucBase;

    @Column(name = "dv_ruc")
    private Integer dvRuc;

    @Column(name = "tipo_contribuyente")
    private Integer tipoContribuyente;

    @Column(name = "timbrado_numero")
    private String timbradoNumero;

    @Column(name = "timbrado_fecha_inicio")
    private LocalDate timbradoFechaInicio;

    @Column(name = "timbrado_fecha_fin")
    private LocalDate timbradoFechaFin;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();
}
