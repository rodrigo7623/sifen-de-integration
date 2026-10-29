package com.tottalstore.sifen.facturacion;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepository extends JpaRepository<FacturaElectronica, UUID> {

    List<FacturaElectronica> findByClienteRucOrderByFechaEmisionDesc(String clienteRuc);

    List<FacturaElectronica> findByEstadoDteOrderByFechaEmisionDesc(EstadoDte estadoDte);

    List<FacturaElectronica> findAllByOrderByFechaEmisionDesc();

    @Query("select f from FacturaElectronica f where f.estadoDte in :estados"
            + " and f.fechaEmision >= :desde and f.fechaEmision < :hasta order by f.fechaEmision")
    List<FacturaElectronica> findEmitidasEntre(
            @Param("estados") Collection<EstadoDte> estados,
            @Param("desde") Instant desde,
            @Param("hasta") Instant hasta);
}
