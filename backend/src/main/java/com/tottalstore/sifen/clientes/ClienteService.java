package com.tottalstore.sifen.clientes;

import com.tottalstore.sifen.clientes.dto.ClienteRequest;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

/** Gestión de clientes (CU-12). */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ValidadorRuc validadorRuc;

    public ClienteService(ClienteRepository clienteRepository, ValidadorRuc validadorRuc) {
        this.clienteRepository = clienteRepository;
        this.validadorRuc = validadorRuc;
    }

    public List<Cliente> buscar(String q, boolean incluirInactivos) {
        if (q == null || q.isBlank()) {
            return incluirInactivos
                    ? clienteRepository.findAllByOrderByRazonSocialAsc()
                    : clienteRepository.findByActivoTrueOrderByRazonSocialAsc();
        }
        return incluirInactivos ? clienteRepository.buscarTodos(q.trim()) : clienteRepository.buscar(q.trim());
    }

    public Cliente obtener(String ruc) {
        return clienteRepository.findById(ruc)
                .orElseThrow(() -> new NotFoundException("Cliente no encontrado: " + ruc));
    }

    public ResultadoValidacionRuc validarRuc(String ruc) {
        return validadorRuc.validar(ruc);
    }

    public Cliente crear(ClienteRequest request) {
        if (clienteRepository.existsById(request.ruc())) {
            // Flujo alternativo 3a del CU-12: RUC duplicado.
            throw new BusinessException("Ya existe un cliente registrado con el RUC/CI " + request.ruc());
        }
        validarRucOFallar(request.ruc());

        Cliente cliente = new Cliente();
        aplicar(cliente, request);
        return clienteRepository.save(cliente);
    }

    public Cliente editar(String ruc, ClienteRequest request) {
        Cliente cliente = obtener(ruc);
        aplicar(cliente, request);
        return clienteRepository.save(cliente);
    }

    public void desactivar(String ruc) {
        Cliente cliente = obtener(ruc);
        cliente.setActivo(false);
        clienteRepository.save(cliente);
    }

    private void validarRucOFallar(String ruc) {
        ResultadoValidacionRuc resultado = validadorRuc.validar(ruc);
        if (!resultado.valido()) {
            // Flujo alternativo 3b del CU-12: RUC inválido.
            throw new BusinessException(resultado.mensaje());
        }
    }

    private void aplicar(Cliente cliente, ClienteRequest request) {
        cliente.setRuc(request.ruc());
        cliente.setRazonSocial(request.razonSocial());
        cliente.setDireccion(request.direccion());
        cliente.setEmail(request.email());
        cliente.setCondicionIva(request.condicionIva());
    }
}
