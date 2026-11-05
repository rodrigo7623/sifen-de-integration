package com.tottalstore.sifen.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class EnviadorEmailSmtpTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final ConfiguracionEmisorService configuracion = mock(ConfiguracionEmisorService.class);

    @SuppressWarnings("unchecked")
    private EnviadorEmailSmtp enviador() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        ConfiguracionEmisor emisor = new ConfiguracionEmisor();
        emisor.setRazonSocial("TOTTAL STORE S.A.");
        when(configuracion.obtenerOCrear()).thenReturn(emisor);
        return new EnviadorEmailSmtp(provider, configuracion, "facturas@tottalstore.com", "Tottal Store");
    }

    private static FacturaElectronica facturaAprobada(String email) {
        Establecimiento est = new Establecimiento();
        est.setCodigo("001");
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setCodigo("001");
        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-6");
        cliente.setRazonSocial("Distribuidora Sur SA");
        cliente.setEmail(email);
        FacturaElectronica f = new FacturaElectronica();
        f.setId(UUID.randomUUID());
        f.setTipoDoc(TipoDocumento.FE);
        f.setEstablecimiento(est);
        f.setPuntoExpedicion(punto);
        f.setNumeroDocumento(5);
        f.setCdc("01043096522001001000000512026110218000177288");
        f.setCliente(cliente);
        f.setTotalGeneral(new BigDecimal("1280000"));
        return f;
    }

    @Test
    void elCorreoLlevaElNumeroDelComprobanteYNoElId() {
        FacturaElectronica factura = facturaAprobada("a@b.com");
        CorreoDocumento correo = CorreoDocumento.de(factura, "TOTTAL STORE S.A.");

        assertThat(correo.asunto()).isEqualTo("Factura electrónica N.° 001-001-0000005 — TOTTAL STORE S.A.");
        assertThat(correo.cuerpo())
                .contains("001-001-0000005")
                .contains("Gs. 1.280.000")
                .contains("01043096522001001000000512026110218000177288")
                .doesNotContain(factura.getId().toString());
        assertThat(correo.nombrePdf()).isEqualTo("factura-001-001-0000005.pdf");
    }

    @Test
    void mandaElPdfYElXmlAlEmailDelCliente() throws Exception {
        EnviadorEmailSmtp enviador = enviador();

        enviador.enviar(facturaAprobada("compras@distribuidorasur.com.py"), new byte[] {1, 2, 3}, "<rDE/>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensaje = captor.getValue();
        assertThat(((InternetAddress) mensaje.getAllRecipients()[0]).getAddress()).isEqualTo("compras@distribuidorasur.com.py");
        assertThat(((InternetAddress) mensaje.getFrom()[0]).getAddress()).isEqualTo("facturas@tottalstore.com");
        assertThat(mensaje.getSubject()).contains("001-001-0000005");
        assertThat(adjuntos(mensaje)).containsExactly("factura-001-001-0000005.pdf", "factura-001-001-0000005.xml");
    }

    @Test
    void sinEmailDelClienteNoIntentaEnviar() {
        EnviadorEmailSmtp enviador = enviador();

        assertThatThrownBy(() -> enviador.enviar(facturaAprobada(null), new byte[0], "<rDE/>"))
                .hasMessageContaining("no tiene email");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void unaFallaDelServidorSmtpSePropagaConElDestinatario() {
        EnviadorEmailSmtp enviador = enviador();
        org.mockito.Mockito.doThrow(new MailSendException("535 credenciales inválidas"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> enviador.enviar(facturaAprobada("a@b.com"), new byte[0], "<rDE/>"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a@b.com")
                .hasMessageContaining("535");
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinServidorConfiguradoFallaAlArrancarConUnMensajeClaro() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);

        assertThatThrownBy(() -> new EnviadorEmailSmtp(provider, configuracion, "x@y.com", "Tottal Store"))
                .hasMessageContaining("SPRING_MAIL_HOST");
    }

    private static List<String> adjuntos(MimeMessage mensaje) throws Exception {
        mensaje.saveChanges();
        List<String> nombres = new ArrayList<>();
        recorrer((Multipart) mensaje.getContent(), nombres);
        return nombres;
    }

    private static void recorrer(Multipart multipart, List<String> nombres) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            Part parte = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(parte.getDisposition())) {
                nombres.add(parte.getFileName());
            } else if (parte.getContent() instanceof Multipart interno) {
                recorrer(interno, nombres);
            }
        }
    }
}
