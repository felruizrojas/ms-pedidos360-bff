package cl.duoc.pedidos360.bff.dto;

import java.time.Instant;

/** Un cambio de estado; estadoAnterior es null en la creación. */
public record HistorialEstadoDto(
        String estadoAnterior,
        String estadoNuevo,
        String usuarioId,
        String usuarioNombre,
        String usuarioRol,
        Instant fecha,
        String comentario) {
}
