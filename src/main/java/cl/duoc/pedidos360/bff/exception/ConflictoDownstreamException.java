package cl.duoc.pedidos360.bff.exception;

import java.util.Map;

import lombok.Getter;

/**
 * El microservicio respondió 409 (transición inválida, sin stock, conflicto de versión): se reenvían su
 * mensaje y sus detalles tal cual. Los detalles son Object porque pueden traer números
 * (p. ej. {"productoId":1,"solicitado":5,"disponible":2}); null si no vinieron.
 */
@Getter
public class ConflictoDownstreamException extends RuntimeException {

    private final transient Map<String, Object> detalles;

    public ConflictoDownstreamException(String mensaje, Map<String, Object> detalles) {
        super(mensaje);
        this.detalles = detalles;
    }
}
