-- Integración real al SIFEN, Fases 3 y 4 (XML real del DE + firma XMLDSig): cada factura recibe un
-- número de documento propio de su punto de expedición (C007 dNumDoc del Manual Técnico v150) y el
-- CDC/código de seguridad con los que se firmó quedan persistidos, en vez de recalcularse en cada
-- envío.

alter table punto_expedicion
    add column ultimo_numero_documento integer not null default 0;

alter table factura_electronica
    add column numero_documento integer,
    add column cdc varchar(44),
    add column codigo_seguridad varchar(9);

create unique index idx_factura_numeracion
    on factura_electronica(punto_expedicion_id, numero_documento)
    where numero_documento is not null;

-- BUG-002: las respuestas/errores reales del SIFEN superan los 250 caracteres.
alter table respuesta_sifen
    alter column descripcion type text;
