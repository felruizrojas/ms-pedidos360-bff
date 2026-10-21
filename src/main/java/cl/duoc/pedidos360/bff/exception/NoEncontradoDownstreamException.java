package cl.duoc.pedidos360.bff.exception;

/** El microservicio respondió 404 (en un client que lo propaga): se reenvía su mensaje. */
public class NoEncontradoDownstreamException extends RuntimeException {

    public NoEncontradoDownstreamException(String mensaje) {
        super(mensaje);
    }
}
