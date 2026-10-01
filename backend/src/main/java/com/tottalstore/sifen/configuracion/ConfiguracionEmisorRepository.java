package com.tottalstore.sifen.configuracion;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionEmisorRepository extends JpaRepository<ConfiguracionEmisor, UUID> {
}
