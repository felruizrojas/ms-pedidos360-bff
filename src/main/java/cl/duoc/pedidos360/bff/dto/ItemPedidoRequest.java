package cl.duoc.pedidos360.bff.dto;

import jakarta.validation.constraints.NotNull;

/** Ítem a pedir. El BFF solo exige que vengan los campos; rangos (cantidad 1..1000, id > 0) los valida orders. */
public record ItemPedidoRequest(
        @NotNull(message = "El productoId es obligatorio.") Long productoId,
        @NotNull(message = "La cantidad es obligatoria.") Integer cantidad) {
}
