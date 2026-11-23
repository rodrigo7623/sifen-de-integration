package com.tottalstore.sifen.sifen;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoSifenRepository extends JpaRepository<EventoSifen, Long> {

    List<EventoSifen> findByFacturaIdOrderByFechaDesc(UUID facturaId);
}
