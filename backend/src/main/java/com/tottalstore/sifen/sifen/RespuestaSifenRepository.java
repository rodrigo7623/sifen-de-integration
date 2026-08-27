package com.tottalstore.sifen.sifen;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaSifenRepository extends JpaRepository<RespuestaSifen, UUID> {
}
