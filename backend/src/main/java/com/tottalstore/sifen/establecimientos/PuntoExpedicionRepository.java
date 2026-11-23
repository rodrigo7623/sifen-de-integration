package com.tottalstore.sifen.establecimientos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuntoExpedicionRepository extends JpaRepository<PuntoExpedicion, UUID>, PuntoExpedicionNumerador {

    boolean existsByEstablecimientoIdAndCodigo(UUID establecimientoId, String codigo);
}
