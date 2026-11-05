package com.tottalstore.sifen.configuracion;

import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.configuracion.dto.ActividadEconomicaRequest;
import com.tottalstore.sifen.configuracion.dto.ConfiguracionEmisorRequest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        reemplazarActividades(config, actividadesDe(request));
        config.setRucBase(request.rucBase());
        config.setDvRuc(request.dvRuc());
        config.setTipoContribuyente(request.tipoContribuyente());
        config.setTimbradoNumero(request.timbradoNumero());
        config.setTimbradoFechaInicio(request.timbradoFechaInicio());
        config.setTimbradoFechaFin(request.timbradoFechaFin());
        config.setUpdatedAt(Instant.now());
        return repository.save(config);
    }

    /** Si el cliente todavía manda el par código/descripción suelto (sin la lista), es la única actividad. */
    private static List<ActividadEconomicaRequest> actividadesDe(ConfiguracionEmisorRequest request) {
        if (request.actividadesEconomicas() != null) {
            return request.actividadesEconomicas();
        }
        if (vacio(request.actividadEconomicaCodigo()) && vacio(request.actividadEconomicaDescripcion())) {
            return List.of();
        }
        return List.of(new ActividadEconomicaRequest(
                request.actividadEconomicaCodigo(), request.actividadEconomicaDescripcion(), true));
    }

    private static void reemplazarActividades(ConfiguracionEmisor config, List<ActividadEconomicaRequest> actividades) {
        if (!actividades.isEmpty()) {
            long principales = actividades.stream().filter(ActividadEconomicaRequest::principal).count();
            if (principales != 1) {
                throw new BusinessException("Marcá exactamente una actividad económica como principal");
            }
            Set<String> codigos = new HashSet<>();
            for (ActividadEconomicaRequest a : actividades) {
                if (vacio(a.codigo()) || vacio(a.descripcion())) {
                    throw new BusinessException("Cada actividad económica necesita código y descripción");
                }
                if (!codigos.add(a.codigo().trim())) {
                    throw new BusinessException("La actividad económica " + a.codigo().trim() + " está repetida");
                }
            }
        }
        // Se reutilizan las filas por código (en vez de borrar todo e insertar de nuevo): Hibernate hace
        // los INSERT antes que los DELETE, y volver a guardar el mismo código chocaría con el unique.
        Map<String, ActividadEconomicaEmisor> existentes = config.getActividadesEconomicas().stream()
                .collect(Collectors.toMap(ActividadEconomicaEmisor::getCodigo, Function.identity(), (a, b) -> a));
        Set<String> pedidos = actividades.stream().map(a -> a.codigo().trim()).collect(Collectors.toSet());
        config.getActividadesEconomicas().removeIf(a -> !pedidos.contains(a.getCodigo()));
        int orden = 0;
        ActividadEconomicaRequest principal = null;
        for (ActividadEconomicaRequest a : actividades) {
            String codigo = a.codigo().trim();
            ActividadEconomicaEmisor actividad = existentes.get(codigo);
            if (actividad == null) {
                actividad = new ActividadEconomicaEmisor(config, codigo, a.descripcion().trim(), a.principal(), orden);
                config.getActividadesEconomicas().add(actividad);
            } else {
                actividad.setDescripcion(a.descripcion().trim());
                actividad.setPrincipal(a.principal());
                actividad.setOrden(orden);
            }
            orden++;
            if (a.principal()) {
                principal = a;
            }
        }
        // Copia de la principal en las columnas de siempre (KuDE, validación de datos del emisor).
        config.setActividadEconomicaCodigo(principal != null ? principal.codigo().trim() : null);
        config.setActividadEconomicaDescripcion(principal != null ? principal.descripcion().trim() : null);
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
