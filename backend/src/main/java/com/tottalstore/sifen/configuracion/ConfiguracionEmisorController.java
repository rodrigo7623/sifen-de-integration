package com.tottalstore.sifen.configuracion;

import com.tottalstore.sifen.configuracion.dto.ConfiguracionEmisorRequest;
import com.tottalstore.sifen.configuracion.dto.ConfiguracionEmisorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lectura abierta a ambos roles; la edición está restringida a ADMIN en {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/configuracion/emisor")
public class ConfiguracionEmisorController {

    private final ConfiguracionEmisorService service;

    public ConfiguracionEmisorController(ConfiguracionEmisorService service) {
        this.service = service;
    }

    @GetMapping
    public ConfiguracionEmisorResponse obtener() {
        return ConfiguracionEmisorResponse.from(service.obtenerOCrear());
    }

    @PutMapping
    public ConfiguracionEmisorResponse actualizar(@RequestBody ConfiguracionEmisorRequest request) {
        return ConfiguracionEmisorResponse.from(service.actualizar(request));
    }
}
