package com.tottalstore.sifen.auditoria;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID>, JpaSpecificationExecutor<LogAuditoria> {

    Page<LogAuditoria> findAllByOrderByFechaHoraDesc(Pageable pageable);

    Page<LogAuditoria> findByFacturaIdOrderByFechaHoraDesc(UUID facturaId, Pageable pageable);

    /** Operaciones que ya aparecen en la auditoría, para el filtro de la pantalla. */
    @Query("select distinct l.operacion from LogAuditoria l order by l.operacion")
    List<String> operacionesRegistradas();
}
