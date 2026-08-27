package com.tottalstore.sifen.clientes;

import com.tottalstore.sifen.clientes.dto.ClienteRequest;
import com.tottalstore.sifen.clientes.dto.ClienteResponse;
import com.tottalstore.sifen.facturacion.FacturaService;
import com.tottalstore.sifen.facturacion.dto.FacturaResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final FacturaService facturaService;

    public ClienteController(ClienteService clienteService, FacturaService facturaService) {
        this.clienteService = clienteService;
        this.facturaService = facturaService;
    }

    @GetMapping
    public List<ClienteResponse> buscar(@RequestParam(required = false) String q) {
        return clienteService.buscar(q).stream().map(ClienteResponse::from).toList();
    }

    @GetMapping("/{ruc}")
    public ClienteResponse obtener(@PathVariable String ruc) {
        return ClienteResponse.from(clienteService.obtener(ruc));
    }

    @GetMapping("/{ruc}/facturas")
    public List<FacturaResponse> historialFacturas(@PathVariable String ruc) {
        return facturaService.historialPorCliente(ruc).stream().map(FacturaResponse::from).toList();
    }

    @GetMapping("/validar-ruc")
    public ResultadoValidacionRuc validarRuc(@RequestParam String ruc) {
        return clienteService.validarRuc(ruc);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.from(clienteService.crear(request));
    }

    @PutMapping("/{ruc}")
    public ClienteResponse editar(@PathVariable String ruc, @Valid @RequestBody ClienteRequest request) {
        return ClienteResponse.from(clienteService.editar(ruc, request));
    }

    @DeleteMapping("/{ruc}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable String ruc) {
        clienteService.desactivar(ruc);
    }
}
