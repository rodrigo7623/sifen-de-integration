package com.tottalstore.sifen.establecimientos;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PuntoExpedicionRepository extends JpaRepository<PuntoExpedicion, UUID> {

    /** Bloquea la fila del punto de expedición para asignar el siguiente número de documento sin
     * que dos confirmaciones concurrentes obtengan el mismo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PuntoExpedicion p where p.id = :id")
    Optional<PuntoExpedicion> findByIdParaNumerar(@Param("id") UUID id);

    boolean existsByEstablecimientoIdAndCodigo(UUID establecimientoId, String codigo);
}
