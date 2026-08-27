package com.tottalstore.sifen.facturacion;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<FacturaElectronica, UUID> {

    List<FacturaElectronica> findByClienteRucOrderByFechaEmisionDesc(String clienteRuc);

    List<FacturaElectronica> findByEstadoDteOrderByFechaEmisionDesc(EstadoDte estadoDte);

    List<FacturaElectronica> findAllByOrderByFechaEmisionDesc();
}
