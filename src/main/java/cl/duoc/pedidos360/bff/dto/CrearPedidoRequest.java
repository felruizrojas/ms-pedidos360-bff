package cl.duoc.pedidos360.bff.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/orders. Solo forma básica con los mismos mensajes que orders (fuente de verdad de negocio):
 * largo de la dirección, máximo de ítems y existencia de productos los valida orders y el BFF reenvía su 400.
 * No se envía precio ni datos del cliente (salen de catalog y del JWT).
 */
public record CrearPedidoRequest(
        @NotBlank(message = "La dirección de entrega es obligatoria.") String direccionEntrega,
        @NotEmpty(message = "El pedido debe tener al menos un ítem.")
        List<@NotNull(message = "Los ítems no pueden ser nulos.") @Valid ItemPedidoRequest> items) {
}
