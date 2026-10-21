package cl.duoc.pedidos360.bff.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;

/**
 * PUT /api/orders/{id}/status. status como texto: orders valida que sea un estado conocido y la transición.
 * comentario opcional (al cancelar queda como motivoCancelacion).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CambiarEstadoRequest(
        @NotBlank(message = "El status es obligatorio.") String status,
        String comentario) {
}
