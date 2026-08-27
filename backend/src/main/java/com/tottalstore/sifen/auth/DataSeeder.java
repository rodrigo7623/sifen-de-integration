package com.tottalstore.sifen.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario administrador de prueba en el primer arranque, para poder acceder al panel
 * sin depender de un flujo de alta de usuarios (fuera del alcance de Release 1).
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String ADMIN_EMAIL = "admin@tottalstore.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador Tottal Store");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRol(Rol.ADMIN);
        admin.setActivo(true);
        usuarioRepository.save(admin);

        log.info("Usuario administrador de prueba creado: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
