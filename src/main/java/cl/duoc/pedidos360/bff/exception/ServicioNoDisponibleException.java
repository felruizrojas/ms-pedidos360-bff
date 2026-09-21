package cl.duoc.pedidos360.bff.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** Microservicio caído (502 si respondió 5xx, 503 si no fue alcanzable o expiró el timeout). */
@Getter
public class ServicioNoDisponibleException extends RuntimeException {

    private final HttpStatus status;

    public ServicioNoDisponibleException(HttpStatus status, String servicio, Throwable cause) {
        super("Servicio de " + servicio + " no disponible", cause);
        this.status = status;
    }
}
