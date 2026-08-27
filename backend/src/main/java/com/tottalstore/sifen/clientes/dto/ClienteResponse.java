package com.tottalstore.sifen.clientes.dto;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.shared.CondicionIva;

public record ClienteResponse(
        String ruc,
        String razonSocial,
        String direccion,
        String email,
        CondicionIva condicionIva,
        boolean activo) {

    public static ClienteResponse from(Cliente c) {
        return new ClienteResponse(
                c.getRuc(), c.getRazonSocial(), c.getDireccion(), c.getEmail(), c.getCondicionIva(), c.isActivo());
    }
}
