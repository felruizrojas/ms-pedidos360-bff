package cl.duoc.pedidos360.bff.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

/** Cuerpo de error uniforme: {timestamp, status, error, mensaje | detalles, path}. */
public final class ErrorBody {

    private ErrorBody() {
    }

    public static Map<String, Object> of(HttpStatus status, String mensaje, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        body.put("path", path);
        return body;
    }

    public static Map<String, Object> of(HttpStatus status, String mensaje, Map<String, String> detalles, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("mensaje", mensaje);
        body.put("detalles", detalles);
        body.put("path", path);
        return body;
    }
}
