package com.tottalstore.sifen.sifen;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnviadorSifenConfigRepository extends JpaRepository<EnviadorSifenConfig, UUID> {
}
