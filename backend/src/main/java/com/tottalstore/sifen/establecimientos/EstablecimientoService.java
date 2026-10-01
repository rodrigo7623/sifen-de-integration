package com.tottalstore.sifen.establecimientos;

import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.establecimientos.dto.EstablecimientoRequest;
import com.tottalstore.sifen.establecimientos.dto.PuntoExpedicionRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestión de establecimientos y sus puntos de expedición (RNF: soporte multi-establecimiento). */
@Service
public class EstablecimientoService {

    private final EstablecimientoRepository establecimientoRepository;
    private final PuntoExpedicionRepository puntoExpedicionRepository;

    public EstablecimientoService(
            EstablecimientoRepository establecimientoRepository,
            PuntoExpedicionRepository puntoExpedicionRepository) {
        this.establecimientoRepository = establecimientoRepository;
        this.puntoExpedicionRepository = puntoExpedicionRepository;
    }

    public List<Establecimiento> listar(boolean incluirInactivos) {
        return incluirInactivos
                ? establecimientoRepository.findAllByOrderByCodigoAsc()
                : establecimientoRepository.findByActivoTrueOrderByCodigoAsc();
    }

    public Establecimiento obtener(UUID id) {
        return establecimientoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Establecimiento no encontrado: " + id));
    }

    @Transactional
    public Establecimiento crear(EstablecimientoRequest request) {
        if (establecimientoRepository.existsByCodigo(request.codigo())) {
            throw new BusinessException("Ya existe un establecimiento con el código " + request.codigo());
        }
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setId(UUID.randomUUID());
        aplicar(establecimiento, request);
        return establecimientoRepository.save(establecimiento);
    }

    @Transactional
    public Establecimiento editar(UUID id, EstablecimientoRequest request) {
        Establecimiento establecimiento = obtener(id);
        if (!establecimiento.getCodigo().equals(request.codigo())
                && establecimientoRepository.existsByCodigo(request.codigo())) {
            throw new BusinessException("Ya existe un establecimiento con el código " + request.codigo());
        }
        aplicar(establecimiento, request);
        return establecimientoRepository.save(establecimiento);
    }

    @Transactional
    public void desactivar(UUID id) {
        Establecimiento establecimiento = obtener(id);
        establecimiento.setActivo(false);
        establecimientoRepository.save(establecimiento);
    }

    @Transactional
    public void activar(UUID id) {
        Establecimiento establecimiento = obtener(id);
        establecimiento.setActivo(true);
        establecimientoRepository.save(establecimiento);
    }

    @Transactional
    public Establecimiento agregarPuntoExpedicion(UUID establecimientoId, PuntoExpedicionRequest request) {
        Establecimiento establecimiento = obtener(establecimientoId);
        if (puntoExpedicionRepository.existsByEstablecimientoIdAndCodigo(establecimientoId, request.codigo())) {
            throw new BusinessException(
                    "El establecimiento " + establecimiento.getCodigo()
                            + " ya tiene un punto de expedición con el código " + request.codigo());
        }
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setId(UUID.randomUUID());
        punto.setEstablecimiento(establecimiento);
        punto.setCodigo(request.codigo());
        punto.setDescripcion(request.descripcion());
        puntoExpedicionRepository.save(punto);
        establecimiento.getPuntosExpedicion().add(punto);
        return establecimiento;
    }

    @Transactional
    public Establecimiento editarPuntoExpedicion(
            UUID establecimientoId, UUID puntoId, PuntoExpedicionRequest request) {
        Establecimiento establecimiento = obtener(establecimientoId);
        PuntoExpedicion punto = obtenerPuntoDe(establecimiento, puntoId);
        if (!punto.getCodigo().equals(request.codigo())
                && puntoExpedicionRepository.existsByEstablecimientoIdAndCodigo(
                        establecimientoId, request.codigo())) {
            throw new BusinessException(
                    "El establecimiento " + establecimiento.getCodigo()
                            + " ya tiene un punto de expedición con el código " + request.codigo());
        }
        punto.setCodigo(request.codigo());
        punto.setDescripcion(request.descripcion());
        puntoExpedicionRepository.save(punto);
        return establecimiento;
    }

    @Transactional
    public Establecimiento cambiarEstadoPuntoExpedicion(UUID establecimientoId, UUID puntoId, boolean activo) {
        Establecimiento establecimiento = obtener(establecimientoId);
        PuntoExpedicion punto = obtenerPuntoDe(establecimiento, puntoId);
        punto.setActivo(activo);
        puntoExpedicionRepository.save(punto);
        return establecimiento;
    }

    private PuntoExpedicion obtenerPuntoDe(Establecimiento establecimiento, UUID puntoId) {
        return establecimiento.getPuntosExpedicion().stream()
                .filter(p -> p.getId().equals(puntoId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "El punto de expedición " + puntoId + " no pertenece al establecimiento "
                                + establecimiento.getCodigo()));
    }

    private void aplicar(Establecimiento establecimiento, EstablecimientoRequest request) {
        establecimiento.setCodigo(request.codigo());
        establecimiento.setDenominacion(request.denominacion());
        establecimiento.setDireccion(request.direccion());
        establecimiento.setNumeroCasa(request.numeroCasa());
        establecimiento.setDepartamentoCodigo(request.departamentoCodigo());
        establecimiento.setDepartamentoDescripcion(request.departamentoDescripcion());
        establecimiento.setDistritoCodigo(request.distritoCodigo());
        establecimiento.setDistritoDescripcion(request.distritoDescripcion());
        establecimiento.setCiudadCodigo(request.ciudadCodigo());
        establecimiento.setCiudadDescripcion(request.ciudadDescripcion());
        establecimiento.setTelefono(request.telefono());
        establecimiento.setEmail(request.email());
    }
}
