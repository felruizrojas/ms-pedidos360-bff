package cl.duoc.pedidos360.bff.dto;

import java.time.Instant;

/**
 * Un paso de la mensajería de un pedido (orders): comando (email | kitchen | invoice) y su estado
 * (ENVIADO | PROCESADO | FALLIDO | EN_DLQ), con el motivo si falló y quién hizo la acción.
 */
public record MensajeComandoDto(
        String comando,
        String type,
        String estado,
        String motivo,
        String usuarioNombre,
        String usuarioRol,
        String comandoEventId,
        Instant fecha) {
}
