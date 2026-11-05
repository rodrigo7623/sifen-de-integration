-- Registro de comprobantes que NO pasan por el SIFEN (RG 90/2021, obligación 955): las compras de
-- Tottal Store (con su crédito fiscal) y las ventas hechas con comprobantes preimpresos o tickets.
-- Los DE aprobados por el SIFEN no van acá: la SET ya los tiene y se excluyen del archivo de Marangatu.

create table comprobante_registrado (
    id                    uuid primary key,
    tipo_registro         varchar(10)  not null check (tipo_registro in ('VENTA', 'COMPRA')),
    tipo_comprobante      varchar(30)  not null,
    fecha_emision         date         not null,
    tipo_identificacion   varchar(30)  not null,
    identificacion        varchar(20)  not null,
    razon_social          varchar(250),
    timbrado              varchar(8)   not null,
    numero                varchar(20),
    cdc                   varchar(44),
    condicion             varchar(10)  not null default 'CONTADO',
    moneda_extranjera     boolean      not null default false,
    gravado10             numeric(18,0) not null default 0,
    gravado5              numeric(18,0) not null default 0,
    exento                numeric(18,0) not null default 0,
    total                 numeric(18,0) not null,
    iva10                 numeric(18,0) not null default 0,
    iva5                  numeric(18,0) not null default 0,
    imputa_iva            boolean      not null default true,
    imputa_ire            boolean      not null default false,
    imputa_irp            boolean      not null default false,
    no_imputa             boolean      not null default false,
    asociado_numero       varchar(20),
    asociado_timbrado     varchar(8),
    observacion           varchar(250),
    creado_por            uuid references usuario(id_usuario),
    modificado_por        uuid references usuario(id_usuario),
    created_at            timestamptz  not null default now(),
    updated_at            timestamptz  not null default now(),
    check (total > 0)
);

create index idx_comprobante_registrado_periodo on comprobante_registrado (tipo_registro, fecha_emision);

-- El mismo comprobante del mismo emisor no se registra dos veces (los tickets sin número quedan fuera).
create unique index ux_comprobante_registrado
    on comprobante_registrado (tipo_registro, tipo_comprobante, identificacion, timbrado, numero)
    where numero is not null;

-- Identificador de 5 posiciones (XXXXX) del nombre del archivo que se importa en Marangatu:
-- tiene que cambiar en cada archivo.
create sequence exportacion_rg90_seq start 1;
