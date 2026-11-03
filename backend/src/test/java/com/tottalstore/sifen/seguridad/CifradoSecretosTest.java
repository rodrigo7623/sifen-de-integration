package com.tottalstore.sifen.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class CifradoSecretosTest {

    private final String claveMaestra = CifradoSecretos.generarClaveMaestra();
    private final CifradoSecretos cifrado = new CifradoSecretos(claveMaestra);

    @Test
    void cifraYDescifraUnSecreto() {
        String valor = cifrado.cifrar("contraseña-del-certificado");

        assertThat(valor).startsWith("ENC(").endsWith(")").doesNotContain("contraseña");
        assertThat(cifrado.descifrar(valor)).isEqualTo("contraseña-del-certificado");
    }

    @Test
    void cadaCifradoUsaUnIvDistinto() {
        assertThat(cifrado.cifrar("mismo valor")).isNotEqualTo(cifrado.cifrar("mismo valor"));
    }

    @Test
    void unValorSinFormatoEncSeDevuelveTalCual() {
        assertThat(cifrado.descifrar("texto-plano")).isEqualTo("texto-plano");
        assertThat(CifradoSecretos.estaCifrado("texto-plano")).isFalse();
    }

    @Test
    void conOtraClaveMaestraNoDescifra() {
        String valor = cifrado.cifrar("secreto");
        CifradoSecretos otra = new CifradoSecretos(CifradoSecretos.generarClaveMaestra());

        assertThatThrownBy(() -> otra.descifrar(valor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clave maestra no corresponde");
    }

    @Test
    void unValorAlteradoNoDescifra() {
        String valor = cifrado.cifrar("secreto");
        // Se altera un byte del texto cifrado (después del IV de 12 bytes), no un carácter del base64,
        // que podría caer en los bits de relleno y no cambiar nada.
        byte[] datos = Base64.getDecoder().decode(valor.substring(4, valor.length() - 1));
        datos[14] ^= 0x01;
        String alterado = "ENC(" + Base64.getEncoder().encodeToString(datos) + ")";

        assertThatThrownBy(() -> cifrado.descifrar(alterado)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaUnaClaveMaestraDeLargoIncorrecto() {
        assertThatThrownBy(() -> new CifradoSecretos("YWJj"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("256 bits");
    }

    @Test
    void elPostProcessorPublicaLosSecretosDescifrados() {
        StandardEnvironment environment = entornoCon(Map.of(
                "SIFEN_MASTER_KEY", claveMaestra,
                "SIFEN_FIRMA_KEYSTORE_PASSWORD", cifrado.cifrar("clave-pfx"),
                "sifen.firma.keystore-password", "${SIFEN_FIRMA_KEYSTORE_PASSWORD:}",
                "otra.propiedad", "sin cifrar"));

        new DescifradoSecretosPostProcessor(new DeferredLogs()).postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("sifen.firma.keystore-password")).isEqualTo("clave-pfx");
        assertThat(environment.getProperty("otra.propiedad")).isEqualTo("sin cifrar");
    }

    @Test
    void elPostProcessorFallaSiHayUnSecretoCifradoYNoHayClaveMaestra() {
        StandardEnvironment environment = entornoCon(Map.of("SIFEN_FIRMA_KEYSTORE_PASSWORD", cifrado.cifrar("clave-pfx")));

        assertThatThrownBy(() -> new DescifradoSecretosPostProcessor(new DeferredLogs())
                        .postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SIFEN_MASTER_KEY");
    }

    private StandardEnvironment entornoCon(Map<String, Object> propiedades) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("prueba", propiedades));
        // Como en la aplicación real: la resolución pasa por las ConfigurationPropertySources de Boot.
        ConfigurationPropertySources.attach(environment);
        return environment;
    }
}
