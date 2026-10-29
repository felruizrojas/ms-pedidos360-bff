package cl.duoc.pedidos360.bff.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * PedidoResponse de orders. estado ∈ CREADO | ACEPTADO | EN_PREPARACION | DESPACHADO | ENTREGADO | CANCELADO
 * (texto, para no acoplar el BFF a la máquina de estados). version: control de concurrencia optimista.
 */
public record PedidoDto(
        Long id,
        String clienteId,
        String clienteNombre,
        String clienteEmail,
        String direccionEntrega,
        String estado,
        BigDecimal total,
        Boolean stockDescontado,
        List<ItemPedidoDto> items,
        Instant creadoEn,
        Instant aceptadoEn,
        Instant enPreparacionEn,
        Instant despachadoEn,
        Instant entregadoEn,
        Instant canceladoEn,
        String motivoCancelacion,
        Long version,
        /** Borrado lógico: se muestra como Eliminado y no se puede modificar. */
        Boolean eliminado,
        Instant eliminadoEn,
        String eliminadoPor) {
}
