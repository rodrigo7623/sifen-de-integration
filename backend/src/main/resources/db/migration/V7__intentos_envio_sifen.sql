-- Release 3, reportes de rechazo: respuesta_sifen guarda solo la ÚLTIMA respuesta de cada factura
-- (factura_id es unique y se sobrescribe en cada reenvío), así que no sirve para estadísticas. Esta
-- tabla registra cada intento de envío con su resultado, append-only.

create table intento_envio_sifen (
    id_intento   uuid primary key,
    factura_id   uuid not null references factura_electronica(id_factura),
    fecha        timestamp not null default now(),
    ambiente     varchar(20),
    aprobado     boolean not null,
    codigo       varchar(20),
    descripcion  text
);

create index idx_intento_envio_fecha on intento_envio_sifen(fecha);
create index idx_intento_envio_factura on intento_envio_sifen(factura_id);

-- Las facturas ya enviadas antes de esta migración aportan su última respuesta como único intento
-- conocido (los intentos intermedios se perdieron al sobrescribirse).
insert into intento_envio_sifen (id_intento, factura_id, fecha, ambiente, aprobado, codigo, descripcion)
select gen_random_uuid(), r.factura_id, r.fecha_respuesta, null,
       f.estado_dte in ('APROBADO', 'ANULADO'), r.codigo, r.descripcion
from respuesta_sifen r
join factura_electronica f on f.id_factura = r.factura_id;
