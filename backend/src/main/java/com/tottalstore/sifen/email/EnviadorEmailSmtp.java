package com.tottalstore.sifen.email;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * RF-09 con un servidor SMTP real ({@code sifen.email.modo=smtp}). La conexión se configura con las
 * propiedades estándar de Spring ({@code SPRING_MAIL_HOST}, {@code SPRING_MAIL_PORT},
 * {@code SPRING_MAIL_USERNAME}, {@code SPRING_MAIL_PASSWORD}, que puede ir como {@code ENC(...)}).
 * Una falla lanza excepción: {@code FacturaService} la registra como EMAIL_FALLIDO sin revertir la
 * aprobación del documento (BUG-001).
 */
@Component
@ConditionalOnProperty(name = "sifen.email.modo", havingValue = "smtp")
public class EnviadorEmailSmtp implements EnviadorEmailService {

    private static final Logger log = LoggerFactory.getLogger(EnviadorEmailSmtp.class);

    private final JavaMailSender mailSender;
    private final ConfiguracionEmisorService configuracionEmisorService;
    private final String remitente;
    private final String nombreRemitente;

    public EnviadorEmailSmtp(
            ObjectProvider<JavaMailSender> mailSender,
            ConfiguracionEmisorService configuracionEmisorService,
            @Value("${sifen.email.remitente:}") String remitente,
            @Value("${sifen.email.nombre-remitente:Tottal Store}") String nombreRemitente) {
        this.mailSender = mailSender.getIfAvailable();
        if (this.mailSender == null) {
            throw new IllegalStateException(
                    "sifen.email.modo=smtp necesita el servidor de correo: definí SPRING_MAIL_HOST (y puerto, usuario y contraseña)");
        }
        if (remitente == null || remitente.isBlank()) {
            throw new IllegalStateException("sifen.email.modo=smtp necesita SIFEN_EMAIL_REMITENTE (la dirección que envía)");
        }
        this.configuracionEmisorService = configuracionEmisorService;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    @Override
    public void enviar(FacturaElectronica factura, byte[] pdf, String xml) {
        CorreoDocumento correo = CorreoDocumento.de(factura, configuracionEmisorService.obtenerOCrear().getRazonSocial());
        if (!correo.tieneDestinatario()) {
            throw new IllegalStateException("El cliente no tiene email registrado");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(correo.destinatario().trim());
            helper.setSubject(correo.asunto());
            helper.setText(correo.cuerpo(), false);
            helper.addAttachment(correo.nombrePdf(), new ByteArrayResource(pdf), "application/pdf");
            helper.addAttachment(correo.nombreXml(), new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8)), "application/xml");
            mailSender.send(mensaje);
            log.info("Correo del documento {} enviado a {}", factura.getId(), correo.destinatario());
        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new IllegalStateException("No se pudo enviar el correo a " + correo.destinatario() + ": " + e.getMessage(), e);
        }
    }
}
