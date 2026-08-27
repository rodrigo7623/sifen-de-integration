package com.tottalstore.sifen.auditoria;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, UUID> {
}
