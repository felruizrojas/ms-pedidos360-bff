package cl.duoc.pedidos360.bff.dto;

import java.math.BigDecimal;

public record ItemPedidoDto(
        Long id,
        Long productoId,
        String productoNombre,
        BigDecimal precioUnitario,
        Integer cantidad,
        BigDecimal subtotal) {
}
