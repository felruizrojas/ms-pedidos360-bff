package cl.duoc.pedidos360.bff.exception;

import java.util.Map;

import lombok.Getter;

/** El microservicio respondió 400; se reenvían sus detalles de validación. */
@Getter
public class ValidacionDownstreamException extends RuntimeException {

    private final Map<String, String> detalles;

    public ValidacionDownstreamException(String mensaje, Map<String, String> detalles) {
        super(mensaje);
        this.detalles = detalles;
    }
}
