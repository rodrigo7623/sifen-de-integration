-- El emisor puede tener varias actividades económicas (grupo D2.1 gActEco del Manual Técnico v150,
-- hasta 9 ocurrencias) y una de ellas es la principal: la que se muestra en el KuDE y va primero en
-- el DE. Las columnas actividad_economica_* de configuracion_emisor quedan como copia de la
-- principal, para no tocar a quien ya las lee.

create table actividad_economica_emisor (
    id                uuid primary key,
    configuracion_id  uuid not null references configuracion_emisor(id) on delete cascade,
    codigo            varchar(10) not null,
    descripcion       varchar(300) not null,
    principal         boolean not null default false,
    orden             integer not null default 0,
    unique (configuracion_id, codigo)
);

-- "Una sola principal" lo controla ConfiguracionEmisorService: un índice único parcial chocaría al
-- cambiar la principal, porque Hibernate actualiza las filas de a una.

insert into actividad_economica_emisor (id, configuracion_id, codigo, descripcion, principal, orden)
select gen_random_uuid(), id, trim(actividad_economica_codigo), trim(actividad_economica_descripcion), true, 0
from configuracion_emisor
where coalesce(trim(actividad_economica_codigo), '') <> ''
  and coalesce(trim(actividad_economica_descripcion), '') <> '';
