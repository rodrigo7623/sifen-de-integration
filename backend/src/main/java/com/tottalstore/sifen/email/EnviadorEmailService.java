package com.tottalstore.sifen.email;

import com.tottalstore.sifen.facturacion.FacturaElectronica;

/**
 * Envío de la factura aprobada al correo del cliente, en PDF y XML (RF-09). Detrás de una interfaz
 * para poder reemplazar el stub por un envío real (JavaMailSender + SMTP) sin tocar el resto del
 * sistema, igual que {@code FirmaDigitalService}/{@code EnviadorSifenService}.
 */
public interface EnviadorEmailService {

    void enviar(FacturaElectronica factura, byte[] pdf, String xml);
}
