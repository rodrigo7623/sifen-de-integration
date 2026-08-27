package com.tottalstore.sifen.sifen;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Configuración del envío al SIFEN (entidad ENVIADOR_SIFEN del diagrama ER). Fila única por ahora. */
@Entity
@Table(name = "enviador_sifen")
@Getter
@Setter
@NoArgsConstructor
public class EnviadorSifenConfig {

    @Id
    @Column(name = "id_enviador")
    private UUID id;

    @Column(name = "url_endpoint")
    private String urlEndpoint;

    @Column(name = "max_reintentos")
    private int maxReintentos;

    @Enumerated(EnumType.STRING)
    private Ambiente ambiente;
}
