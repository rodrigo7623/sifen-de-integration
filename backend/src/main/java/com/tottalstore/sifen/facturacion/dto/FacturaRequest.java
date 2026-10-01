package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.CondicionPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record FacturaRequest(
        @NotBlank(message = "El cliente es obligatorio") String clienteRuc,
        @NotNull(message = "El establecimiento es obligatorio") UUID establecimientoId,
        @NotNull(message = "El punto de expedición es obligatorio") UUID puntoExpedicionId,
        @NotNull(message = "La condición de pago es obligatoria") CondicionPago condicionPago,
        Integer plazoDias,
        Integer cantidadCuotas,
        @NotEmpty(message = "La factura debe tener al menos un ítem") @Valid List<ItemFacturaRequest> items) {
}
