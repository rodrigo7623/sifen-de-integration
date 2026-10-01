package com.tottalstore.sifen.auditoria;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {

    Page<LogAuditoria> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<LogAuditoria> findByFacturaIdOrderByFechaHoraDesc(UUID facturaId, Pageable pageable);
}
