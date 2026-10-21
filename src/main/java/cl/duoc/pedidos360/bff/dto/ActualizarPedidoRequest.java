package cl.duoc.pedidos360.bff.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * PUT /api/orders/{id}. Ambos campos opcionales (orders exige al menos uno); los null no se envían, así
 * orders recibe exactamente lo que mandó el front. items, si viene, reemplaza todos los ítems.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActualizarPedidoRequest(
        String direccionEntrega,
        List<@NotNull(message = "Los ítems no pueden ser nulos.") @Valid ItemPedidoRequest> items) {
}
