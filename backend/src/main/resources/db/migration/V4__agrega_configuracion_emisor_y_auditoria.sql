-- Release 2 (UI/pantallas nuevas): tabla de configuración del emisor (fila única, para que el
-- admin cargue razón social/dirección/actividad económica/timbrado desde la UI en vez de variables
-- de entorno) e índice de soporte para el nuevo visor de auditoría.
-- Nombres de columna alineados a los tags del Manual Técnico SIFEN v150 (D105, D107, D111/112,
-- D131/132, etc.) para que mapear esta tabla a la Fase 3 (XML real) sea directo cuando se retome.
-- Todavía NO está conectada a sifen.emisor.* (EmisorConfig) ni al CDC/XML real -- es almacenamiento
-- preparatorio.

create table configuracion_emisor (
    id                           uuid primary key,
    razon_social                 varchar(255),
    nombre_fantasia               varchar(255),
    direccion                    varchar(255),
    numero_casa                  varchar(10),
    complemento_direccion1       varchar(255),
    complemento_direccion2       varchar(255),
    departamento_codigo          varchar(5),
    departamento_descripcion     varchar(50),
    distrito_codigo              varchar(5),
    distrito_descripcion         varchar(50),
    ciudad_codigo                varchar(5),
    ciudad_descripcion           varchar(50),
    telefono                     varchar(20),
    email                        varchar(100),
    actividad_economica_codigo       varchar(10),
    actividad_economica_descripcion  varchar(300),
    ruc_base                     varchar(20),
    dv_ruc                       integer,
    tipo_contribuyente           integer,
    establecimiento              varchar(3),
    punto_expedicion             varchar(3),
    timbrado_numero              varchar(10),
    timbrado_fecha_inicio        date,
    timbrado_fecha_fin           date,
    updated_at                   timestamp not null default now()
);

insert into configuracion_emisor (id) values (gen_random_uuid());

create index idx_log_auditoria_fecha on log_auditoria(fecha_hora desc);
