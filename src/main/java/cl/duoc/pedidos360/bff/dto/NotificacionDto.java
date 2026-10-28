package cl.duoc.pedidos360.bff.dto;

import java.time.Instant;

/** Aviso en la app (pedido cancelado, modificado o eliminado) que guarda orders. */
public record NotificacionDto(
        Long id,
        Long pedidoId,
        String titulo,
        String mensaje,
        Instant fecha,
        boolean leida) {
}
