package com.tottalstore.sifen.configuracion;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una actividad económica del emisor (D2.1 {@code gActEco}: D131 código, D132 descripción). */
@Entity
@Table(name = "actividad_economica_emisor")
@Getter
@Setter
@NoArgsConstructor
public class ActividadEconomicaEmisor {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "configuracion_id")
    private ConfiguracionEmisor configuracion;

    private String codigo;

    private String descripcion;

    /** La principal se muestra en el KuDE y va primera en el DE. Hay exactamente una si hay actividades. */
    private boolean principal;

    private int orden;

    public ActividadEconomicaEmisor(ConfiguracionEmisor configuracion, String codigo, String descripcion,
                                    boolean principal, int orden) {
        this.id = UUID.randomUUID();
        this.configuracion = configuracion;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.principal = principal;
        this.orden = orden;
    }
}
