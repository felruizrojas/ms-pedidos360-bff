package cl.duoc.pedidos360.bff.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/** El microservicio rechazó el token (401) o los permisos (403). */
@Getter
public class AccesoRechazadoDownstreamException extends RuntimeException {

    private final HttpStatus status;

    public AccesoRechazadoDownstreamException(HttpStatus status, String servicio) {
        super(status == HttpStatus.UNAUTHORIZED
                ? "El servicio de " + servicio + " rechazó el token"
                : "No tiene permisos para esta operación en el servicio de " + servicio);
        this.status = status;
    }
}
