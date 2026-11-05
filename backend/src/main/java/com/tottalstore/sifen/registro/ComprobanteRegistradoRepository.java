package com.tottalstore.sifen.registro;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteRegistradoRepository extends JpaRepository<ComprobanteRegistrado, UUID> {

    @Query("""
            select c from ComprobanteRegistrado c
            where c.fechaEmision >= :desde and c.fechaEmision < :hasta
            order by c.fechaEmision asc, c.numero asc, c.createdAt asc
            """)
    List<ComprobanteRegistrado> delPeriodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select c from ComprobanteRegistrado c
            where c.tipoRegistro = :tipo and c.fechaEmision >= :desde and c.fechaEmision < :hasta
            order by c.fechaEmision asc, c.numero asc, c.createdAt asc
            """)
    List<ComprobanteRegistrado> delPeriodo(
            @Param("tipo") TipoRegistro tipo, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    boolean existsByTipoRegistroAndTipoComprobanteAndIdentificacionAndTimbradoAndNumero(
            TipoRegistro tipoRegistro, TipoComprobanteRg90 tipoComprobante, String identificacion, String timbrado,
            String numero);

    boolean existsByTipoRegistroAndTipoComprobanteAndIdentificacionAndTimbradoAndNumeroAndIdNot(
            TipoRegistro tipoRegistro, TipoComprobanteRg90 tipoComprobante, String identificacion, String timbrado,
            String numero, UUID id);

    @Query(value = "select nextval('exportacion_rg90_seq')", nativeQuery = true)
    long siguienteNumeroExportacion();
}
