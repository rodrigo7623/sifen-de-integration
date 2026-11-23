package com.tottalstore.sifen.establecimientos;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.UUID;

class PuntoExpedicionNumeradorImpl implements PuntoExpedicionNumerador {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<PuntoExpedicion> bloquearParaNumerar(UUID id) {
        PuntoExpedicion punto = entityManager.find(PuntoExpedicion.class, id);
        if (punto == null) {
            return Optional.empty();
        }
        // Primero el bloqueo (SELECT ... FOR UPDATE, espera a que la otra confirmación termine) y
        // recién después la relectura. Un refresh(punto, PESSIMISTIC_WRITE) no sirve: como el punto
        // trae su establecimiento con un outer join, PostgreSQL no admite FOR UPDATE en esa consulta
        // y Hibernate lee la fila sin bloqueo y la bloquea después (follow-on locking), con lo que el
        // número leído vuelve a ser el viejo.
        entityManager.lock(punto, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(punto);
        return Optional.of(punto);
    }
}
