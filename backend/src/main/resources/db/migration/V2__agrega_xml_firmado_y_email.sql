-- Release 2 (M3 + envío por correo): persistir el XML firmado que hoy se genera en memoria y se
-- descarta, y marcar si la factura aprobada ya fue enviada por correo al cliente (RF-09, RF-10).

alter table respuesta_sifen
    add column xml_firmado text;

alter table factura_electronica
    add column email_enviado boolean not null default false;
