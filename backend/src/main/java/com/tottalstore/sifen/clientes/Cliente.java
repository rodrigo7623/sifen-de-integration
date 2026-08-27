package com.tottalstore.sifen.clientes;

import com.tottalstore.sifen.shared.CondicionIva;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cliente al que se le pueden emitir facturas (RF-15). */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    private String ruc;

    @Column(name = "razon_social")
    private String razonSocial;

    private String direccion;

    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_iva")
    private CondicionIva condicionIva;

    private boolean activo = true;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
