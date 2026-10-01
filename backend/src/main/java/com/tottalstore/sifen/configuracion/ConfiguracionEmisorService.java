package com.tottalstore.sifen.configuracion;

import com.tottalstore.sifen.configuracion.dto.ConfiguracionEmisorRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracionEmisorService {

    private final ConfiguracionEmisorRepository repository;

    public ConfiguracionEmisorService(ConfiguracionEmisorRepository repository) {
        this.repository = repository;
    }

    /** La migración V4 siembra exactamente una fila; si por algún motivo faltara, se crea aquí. */
    @Transactional
    public ConfiguracionEmisor obtenerOCrear() {
        return repository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    ConfiguracionEmisor nueva = new ConfiguracionEmisor();
                    nueva.setId(UUID.randomUUID());
                    return repository.save(nueva);
                });
    }

    @Transactional
    public ConfiguracionEmisor actualizar(ConfiguracionEmisorRequest request) {
        ConfiguracionEmisor config = obtenerOCrear();
        config.setRazonSocial(request.razonSocial());
        config.setNombreFantasia(request.nombreFantasia());
        config.setDireccion(request.direccion());
        config.setNumeroCasa(request.numeroCasa());
        config.setComplementoDireccion1(request.complementoDireccion1());
        config.setComplementoDireccion2(request.complementoDireccion2());
        config.setDepartamentoCodigo(request.departamentoCodigo());
        config.setDepartamentoDescripcion(request.departamentoDescripcion());
        config.setDistritoCodigo(request.distritoCodigo());
        config.setDistritoDescripcion(request.distritoDescripcion());
        config.setCiudadCodigo(request.ciudadCodigo());
        config.setCiudadDescripcion(request.ciudadDescripcion());
        config.setTelefono(request.telefono());
        config.setEmail(request.email());
        config.setActividadEconomicaCodigo(request.actividadEconomicaCodigo());
        config.setActividadEconomicaDescripcion(request.actividadEconomicaDescripcion());
        config.setRucBase(request.rucBase());
        config.setDvRuc(request.dvRuc());
        config.setTipoContribuyente(request.tipoContribuyente());
        config.setTimbradoNumero(request.timbradoNumero());
        config.setTimbradoFechaInicio(request.timbradoFechaInicio());
        config.setTimbradoFechaFin(request.timbradoFechaFin());
        config.setUpdatedAt(Instant.now());
        return repository.save(config);
    }
}
