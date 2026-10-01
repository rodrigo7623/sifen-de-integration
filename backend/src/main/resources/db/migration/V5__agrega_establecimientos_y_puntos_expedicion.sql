-- El sistema debe soportar múltiples establecimientos de expedición, cada uno con sus propios
-- puntos de expedición (estructura real del SIFEN: grupo D1 Establecimiento, D017 Punto de
-- Expedición). Reemplaza los campos sueltos establecimiento/punto_expedicion que tenía
-- configuracion_emisor (una sola fila) por tablas propias, y cada factura queda asociada al
-- establecimiento/punto de expedición desde el que se emitió.

create table establecimiento (
    id                        uuid primary key,
    codigo                    varchar(3) not null unique,
    denominacion              varchar(100) not null,
    direccion                 varchar(255),
    numero_casa               varchar(10),
    departamento_codigo       varchar(5),
    departamento_descripcion  varchar(50),
    distrito_codigo           varchar(5),
    distrito_descripcion      varchar(50),
    ciudad_codigo             varchar(5),
    ciudad_descripcion        varchar(50),
    telefono                  varchar(20),
    email                     varchar(100),
    activo                    boolean not null default true,
    created_at                timestamp not null default now()
);

create table punto_expedicion (
    id                  uuid primary key,
    establecimiento_id  uuid not null references establecimiento(id),
    codigo              varchar(3) not null,
    descripcion         varchar(100),
    activo              boolean not null default true,
    created_at          timestamp not null default now(),
    unique (establecimiento_id, codigo)
);

-- Establecimiento/punto de expedición por defecto, mismo código provisorio (001/001) que ya se
-- usaba en sifen.emisor.establecimiento/punto-expedicion -- preserva el comportamiento de las
-- facturas existentes.
with nuevo_establecimiento as (
    insert into establecimiento (id, codigo, denominacion)
    values (gen_random_uuid(), '001', 'Casa matriz')
    returning id
)
insert into punto_expedicion (id, establecimiento_id, codigo, descripcion)
select gen_random_uuid(), id, '001', 'Caja principal' from nuevo_establecimiento;

alter table configuracion_emisor drop column establecimiento;
alter table configuracion_emisor drop column punto_expedicion;

alter table factura_electronica add column establecimiento_id uuid references establecimiento(id);
alter table factura_electronica add column punto_expedicion_id uuid references punto_expedicion(id);

update factura_electronica
set establecimiento_id = (select id from establecimiento where codigo = '001'),
    punto_expedicion_id = (select id from punto_expedicion where codigo = '001');

alter table factura_electronica alter column establecimiento_id set not null;
alter table factura_electronica alter column punto_expedicion_id set not null;
