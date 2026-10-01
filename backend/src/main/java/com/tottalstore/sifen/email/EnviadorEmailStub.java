package com.tottalstore.sifen.email;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Implementación stub: en vez de mandar un correo real, escribe el PDF, el XML y un resumen del
 * envío en una carpeta local — así se puede demostrar/inspeccionar qué se "envió" sin credenciales
 * SMTP. TODO: reemplazar por un envío real con JavaMailSender cuando haya credenciales de correo.
 */
@Component
public class EnviadorEmailStub implements EnviadorEmailService {

    private static final Logger log = LoggerFactory.getLogger(EnviadorEmailStub.class);

    private final Path outboxDir;

    public EnviadorEmailStub(@Value("${sifen.email.outbox-dir}") String outboxDir) {
        this.outboxDir = Path.of(outboxDir);
    }

    @Override
    public void enviar(FacturaElectronica factura, byte[] pdf, String xml) {
        try {
            Files.createDirectories(outboxDir);
            String base = "factura-" + factura.getId();
            String destinatario = factura.getCliente().getEmail();

            Files.write(outboxDir.resolve(base + ".pdf"), pdf);
            Files.writeString(outboxDir.resolve(base + ".xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(
                    outboxDir.resolve(base + ".txt"),
                    "Para: " + (destinatario != null ? destinatario : "(cliente sin email registrado)") + "\n"
                            + "Asunto: Tu factura electrónica de Tottal Store\n"
                            + "Cuerpo: Adjuntamos tu factura N.° " + factura.getId()
                            + " por un total de " + factura.getTotalGeneral() + " Gs., en PDF y XML.\n"
                            + "(Este correo es SIMULADO — no se envió nada real; ver " + base + ".pdf/.xml)\n",
                    StandardCharsets.UTF_8);

            log.info("Email simulado escrito en {} para la factura {}", outboxDir, factura.getId());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo escribir el email simulado de la factura " + factura.getId(), e);
        }
    }
}
