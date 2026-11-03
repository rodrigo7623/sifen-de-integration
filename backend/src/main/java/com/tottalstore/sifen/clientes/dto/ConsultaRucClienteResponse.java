package com.tottalstore.sifen.clientes.dto;

import com.tottalstore.sifen.shared.CondicionIva;

/**
 * Resultado de buscar un RUC al dar de alta un cliente o al cargar una factura: si ya está
 * registrado, el cliente; si no, los datos que devuelve el SIFEN (siConsRUC) para precargar el alta.
 *
 * @param ruc RUC normalizado base-DV (el DV se calcula si el usuario ingresó solo el número)
 * @param dvCalculado true si el DV no venía en lo ingresado y lo calculó el sistema
 * @param cliente el cliente ya registrado con ese RUC, o null
 * @param consultadoEnSifen true si el SIFEN llegó a responder
 * @param existeEnSifen true (0502), false (0500), o null si no se pudo consultar
 * @param razonSocial razón social del SIFEN, o la del cliente registrado
 * @param estado estado del contribuyente en el SIFEN (ACTIVO, SUSPENDIDO, …)
 * @param facturadorElectronico si el contribuyente está habilitado como facturador electrónico
 * @param condicionIvaSugerida contribuyente → RESPONSABLE_IVA; no inscripto → CONSUMIDOR_FINAL
 * @param mensaje explicación para mostrar al usuario
 */
public record ConsultaRucClienteResponse(
        String ruc,
        boolean dvCalculado,
        ClienteResponse cliente,
        boolean consultadoEnSifen,
        Boolean existeEnSifen,
        String razonSocial,
        String estado,
        Boolean facturadorElectronico,
        CondicionIva condicionIvaSugerida,
        String mensaje) {
}
