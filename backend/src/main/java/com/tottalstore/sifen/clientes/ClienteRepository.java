package com.tottalstore.sifen.clientes;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    List<Cliente> findByActivoTrueOrderByRazonSocialAsc();

    List<Cliente> findAllByOrderByRazonSocialAsc();

    @Query("""
            select c from Cliente c
            where c.activo = true
            and (lower(c.ruc) like lower(concat('%', :q, '%'))
                 or lower(c.razonSocial) like lower(concat('%', :q, '%'))
                 or lower(c.email) like lower(concat('%', :q, '%')))
            order by c.razonSocial asc
            """)
    List<Cliente> buscar(@Param("q") String q);

    @Query("""
            select c from Cliente c
            where lower(c.ruc) like lower(concat('%', :q, '%'))
               or lower(c.razonSocial) like lower(concat('%', :q, '%'))
               or lower(c.email) like lower(concat('%', :q, '%'))
            order by c.razonSocial asc
            """)
    List<Cliente> buscarTodos(@Param("q") String q);
}
