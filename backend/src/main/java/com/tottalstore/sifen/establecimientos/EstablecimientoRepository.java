package com.tottalstore.sifen.establecimientos;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoRepository extends JpaRepository<Establecimiento, UUID> {

    List<Establecimiento> findAllByOrderByCodigoAsc();

    List<Establecimiento> findByActivoTrueOrderByCodigoAsc();

    boolean existsByCodigo(String codigo);
}
