-- M6 (Release 3): notas de crédito y débito electrónicas (C002 = 5 / 6 del Manual Técnico v150).
-- Una nota es un DE más, con el mismo ciclo de vida (firma, envío, KuDE, Libro IVA) que la factura:
-- se guarda en factura_electronica con su tipo_doc y la referencia a la factura que ajusta (grupo H,
-- gCamDEAsoc). Relación 1:N: una factura puede tener varias notas.

alter table factura_electronica
    add column factura_asociada_id uuid references factura_electronica(id_factura),
    add column motivo_emision varchar(40);

-- E7 (condición de la operación) solo existe en la factura: las notas no la informan.
alter table factura_electronica
    alter column condicion_pago drop not null;

create index idx_factura_asociada on factura_electronica(factura_asociada_id);

-- Cada tipo de documento tiene su propia numeración dentro del punto de expedición.
alter table punto_expedicion
    add column ultimo_numero_nc integer not null default 0,
    add column ultimo_numero_nd integer not null default 0;

drop index idx_factura_numeracion;
create unique index idx_factura_numeracion
    on factura_electronica(punto_expedicion_id, tipo_doc, numero_documento)
    where numero_documento is not null;
