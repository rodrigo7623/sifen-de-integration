package com.tottalstore.sifen.catalogo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, String> {

    List<Producto> findByActivoTrueOrderByDescripcionAsc();

    @Query("""
            select p from Producto p
            where p.activo = true
            and (lower(p.codigo) like lower(concat('%', :q, '%'))
                 or lower(p.descripcion) like lower(concat('%', :q, '%')))
            order by p.descripcion asc
            """)
    List<Producto> buscar(@Param("q") String q);
}
