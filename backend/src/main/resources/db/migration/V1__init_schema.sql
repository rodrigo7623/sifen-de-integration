-- Esquema inicial (Release 1): infraestructura, autenticación, catálogo, clientes,
-- factura manual, y las tablas de soporte para firma digital / envío SIFEN (stubs por ahora).
-- Las tablas de PEDIDO (M2), LIBRO_IVA (M4) y NOTA_CREDITO_DEBITO (M6) se agregan en
-- migraciones posteriores cuando se implementen esos módulos.

create table usuario (
    id_usuario    uuid primary key,
    nombre        varchar(150) not null,
    email         varchar(150) not null unique,
    password_hash varchar(255) not null,
    rol           varchar(20) not null,
    activo        boolean not null default true,
    created_at    timestamp not null default now()
);

create table cliente (
    ruc            varchar(20) primary key,
    razon_social   varchar(200) not null,
    direccion      varchar(250),
    email          varchar(150),
    condicion_iva  varchar(30) not null,
    activo         boolean not null default true,
    created_at     timestamp not null default now()
);

create table producto (
    codigo         varchar(50) primary key,
    descripcion    varchar(200) not null,
    unidad_medida  varchar(20) not null,
    precio_base    numeric(15,2) not null,
    tasa_iva       varchar(10) not null,
    activo         boolean not null default true,
    created_at     timestamp not null default now()
);

create table factura_electronica (
    id_factura       uuid primary key,
    tipo_doc         varchar(20) not null default 'FE',
    estado_dte       varchar(20) not null,
    total_iva5       numeric(15,2) not null default 0,
    total_iva10      numeric(15,2) not null default 0,
    total_general    numeric(15,2) not null default 0,
    condicion_pago   varchar(20) not null,
    plazo_dias       integer,
    cantidad_cuotas  integer,
    fecha_emision    timestamp not null default now(),
    cliente_ruc      varchar(20) not null references cliente(ruc),
    usuario_id       uuid not null references usuario(id_usuario)
);

create index idx_factura_cliente on factura_electronica(cliente_ruc);
create index idx_factura_estado on factura_electronica(estado_dte);

create table item_factura (
    id_item          uuid primary key,
    descripcion      varchar(200) not null,
    cantidad         integer not null,
    precio_unitario  numeric(15,2) not null,
    tasa_iva         varchar(10) not null,
    subtotal         numeric(15,2) not null,
    factura_id       uuid not null references factura_electronica(id_factura) on delete cascade,
    producto_codigo  varchar(50) references producto(codigo)
);

create index idx_item_factura on item_factura(factura_id);

create table enviador_sifen (
    id_enviador     uuid primary key,
    url_endpoint    varchar(250),
    max_reintentos  integer not null default 3,
    ambiente        varchar(20) not null default 'TEST'
);

create table respuesta_sifen (
    id_respuesta     uuid primary key,
    codigo           varchar(20),
    descripcion      varchar(250),
    cdc              varchar(50),
    fecha_respuesta  timestamp not null default now(),
    factura_id       uuid not null unique references factura_electronica(id_factura)
);

create table log_auditoria (
    id_log      uuid primary key,
    operacion   varchar(100) not null,
    fecha_hora  timestamp not null default now(),
    usuario_id  uuid references usuario(id_usuario),
    factura_id  uuid references factura_electronica(id_factura)
);

create index idx_log_factura on log_auditoria(factura_id);

insert into enviador_sifen (id_enviador, url_endpoint, max_reintentos, ambiente)
values (gen_random_uuid(), 'https://sifen-test.set.gov.py (pendiente de configurar)', 3, 'TEST');
