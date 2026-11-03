package com.tottalstore.sifen.seguridad;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * Descifra, al arrancar, toda propiedad cuyo valor tenga la forma {@code ENC(...)} (variables de
 * entorno, {@code .env} cargado por {@code iniciar-backend.ps1}, {@code application.yml}) y la
 * publica descifrada en una fuente de mayor prioridad. Así los consumidores (certificado, CSC,
 * datasource) siguen leyendo {@code @Value} normales y el secreto en claro nunca se guarda en disco.
 *
 * <p>En modo real, si la contraseña del certificado está en texto plano, deja una advertencia: se
 * admite para no romper instalaciones existentes, pero no cumple RNF-03.
 */
public class DescifradoSecretosPostProcessor implements EnvironmentPostProcessor, Ordered {

    // SystemEnvironmentPropertySource con nombre "*-systemEnvironment": Spring Boot le aplica el mapeo de
    // nombres estilo variable de entorno (SIFEN_FIRMA_KEYSTORE_PASSWORD), que una MapPropertySource común
    // no tiene, y la propiedad descifrada quedaría oculta detrás de la original.
    static final String FUENTE = "secretosDescifrados-systemEnvironment";
    static final String CLAVE_MAESTRA = "SIFEN_MASTER_KEY";
    private static final String PASSWORD_CERTIFICADO = "SIFEN_FIRMA_KEYSTORE_PASSWORD";

    private final Log log;

    public DescifradoSecretosPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(DescifradoSecretosPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> descifrados = new HashMap<>();
        CifradoSecretos cifrado = null;
        for (PropertySource<?> fuente : environment.getPropertySources()) {
            if (!(fuente instanceof EnumerablePropertySource<?> enumerable)) {
                continue;
            }
            for (String nombre : enumerable.getPropertyNames()) {
                if (descifrados.containsKey(nombre)
                        || !(enumerable.getProperty(nombre) instanceof CharSequence valor)
                        || !CifradoSecretos.estaCifrado(valor.toString())) {
                    continue;
                }
                if (cifrado == null) {
                    String claveMaestra = environment.getProperty(CLAVE_MAESTRA);
                    if (claveMaestra == null || claveMaestra.isBlank()) {
                        throw new IllegalStateException("La propiedad '" + nombre
                                + "' está cifrada (ENC(...)) pero no está definida la variable de entorno " + CLAVE_MAESTRA);
                    }
                    cifrado = new CifradoSecretos(claveMaestra);
                }
                descifrados.put(nombre, cifrado.descifrar(valor.toString()));
            }
        }
        if (!descifrados.isEmpty()) {
            environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(FUENTE, descifrados));
            log.info("RNF-03: " + descifrados.size() + " secreto(s) de configuración descifrado(s) en memoria");
        }
        advertirSiPasswordEnClaro(environment, descifrados);
    }

    private void advertirSiPasswordEnClaro(ConfigurableEnvironment environment, Map<String, Object> descifrados) {
        if (!"real".equals(environment.getProperty("sifen.envio.modo"))) {
            return;
        }
        String password = environment.getProperty(PASSWORD_CERTIFICADO);
        if (password != null && !password.isBlank() && !descifrados.containsKey(PASSWORD_CERTIFICADO)) {
            log.warn("RNF-03: SIFEN_FIRMA_KEYSTORE_PASSWORD está en texto plano. Cifrala con "
                    + "'java -cp target/classes com.tottalstore.sifen.seguridad.CifradoSecretos cifrar' y guardá el ENC(...)");
        }
    }

    /** Después de cargar application.yml y las demás fuentes estándar. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
