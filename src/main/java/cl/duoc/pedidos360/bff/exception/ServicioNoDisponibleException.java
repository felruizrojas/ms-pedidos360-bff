package cl.duoc.pedidos360.bff.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Microservicio caído (502 si respondió 5xx, 503 si no fue alcanzable o expiró el timeout, o si respondió
 * 503 en un client que lo propaga, p. ej. orders con catalog caído).
 */
@Getter
public class ServicioNoDisponibleException extends RuntimeException {

    private final HttpStatus status;

    public ServicioNoDisponibleException(HttpStatus status, String servicio, Throwable cause) {
        super("Servicio de " + servicio + " no disponible", cause);
        this.status = status;
    }

    /** Con el mensaje que envió el microservicio (p. ej. "Servicio de catálogo no disponible" desde orders). */
    public static ServicioNoDisponibleException conMensaje(HttpStatus status, String mensaje) {
        return new ServicioNoDisponibleException(status, mensaje);
    }

    private ServicioNoDisponibleException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }
}
