package com.tottalstore.sifen.sifen;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntentoEnvioSifenRepository extends JpaRepository<IntentoEnvioSifen, UUID> {

    List<IntentoEnvioSifen> findByFechaGreaterThanEqualAndFechaLessThanOrderByFechaAsc(Instant desde, Instant hasta);

    List<IntentoEnvioSifen> findByFacturaClienteRucOrderByFechaAsc(String clienteRuc);
}
