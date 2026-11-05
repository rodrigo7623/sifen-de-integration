package com.tottalstore.sifen.email;

import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación stub ({@code sifen.email.modo=stub}, el default): en vez de mandar un correo real,
 * escribe el PDF, el XML y el correo armado (el mismo asunto y cuerpo que enviaría
 * {@link EnviadorEmailSmtp}) en una carpeta local, para demostrar qué se enviaría sin credenciales SMTP.
 */
@Component
@ConditionalOnProperty(name = "sifen.email.modo", havingValue = "stub", matchIfMissing = true)
public class EnviadorEmailStub implements EnviadorEmailService {

    private static final Logger log = LoggerFactory.getLogger(EnviadorEmailStub.class);

    private final Path outboxDir;
    private final ConfiguracionEmisorService configuracionEmisorService;

    public EnviadorEmailStub(
            @Value("${sifen.email.outbox-dir}") String outboxDir, ConfiguracionEmisorService configuracionEmisorService) {
        this.outboxDir = Path.of(outboxDir);
        this.configuracionEmisorService = configuracionEmisorService;
    }

    @Override
    public void enviar(FacturaElectronica factura, byte[] pdf, String xml) {
        CorreoDocumento correo = CorreoDocumento.de(factura, configuracionEmisorService.obtenerOCrear().getRazonSocial());
        if (!correo.tieneDestinatario()) {
            throw new IllegalStateException("El cliente no tiene email registrado");
        }
        try {
            Files.createDirectories(outboxDir);
            String base = correo.nombrePdf().replaceFirst("\\.pdf$", "");
            Files.write(outboxDir.resolve(correo.nombrePdf()), pdf);
            Files.writeString(outboxDir.resolve(correo.nombreXml()), xml, StandardCharsets.UTF_8);
            Files.writeString(
                    outboxDir.resolve(base + ".txt"),
                    "Para: " + correo.destinatario() + "\n"
                            + "Asunto: " + correo.asunto() + "\n"
                            + "Adjuntos: " + correo.nombrePdf() + ", " + correo.nombreXml() + "\n\n"
                            + correo.cuerpo() + "\n"
                            + "(Correo SIMULADO: no se envió nada. Para enviarlo de verdad, sifen.email.modo=smtp.)\n",
                    StandardCharsets.UTF_8);
            log.info("Email simulado escrito en {} para el documento {}", outboxDir, factura.getId());
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo escribir el email simulado del documento " + factura.getId(), e);
        }
    }
}
