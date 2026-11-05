package com.tottalstore.sifen.facturacion;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<FacturaElectronica, UUID> {

    /** Bloquea la fila hasta el fin de la transacción: dos confirmaciones simultáneas de la misma
     * factura se atienden de a una, y la segunda ya ve el estado que dejó la primera (BUG-004). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FacturaElectronica f where f.id = :id")
    Optional<FacturaElectronica> findByIdParaActualizar(@Param("id") UUID id);

    List<FacturaElectronica> findByClienteRucOrderByFechaEmisionDesc(String clienteRuc);

    List<FacturaElectronica> findByEstadoDteOrderByFechaEmisionDesc(EstadoDte estadoDte);

    List<FacturaElectronica> findAllByOrderByFechaEmisionDesc();

    /** Notas de crédito/débito emitidas sobre una factura (M6). */
    List<FacturaElectronica> findByFacturaAsociadaIdOrderByFechaEmisionAsc(UUID facturaAsociadaId);

    @Query("select f from FacturaElectronica f where f.estadoDte in :estados"
            + " and f.fechaEmision >= :desde and f.fechaEmision < :hasta order by f.fechaEmision")
    List<FacturaElectronica> findEmitidasEntre(
            @Param("estados") Collection<EstadoDte> estados,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);
}
