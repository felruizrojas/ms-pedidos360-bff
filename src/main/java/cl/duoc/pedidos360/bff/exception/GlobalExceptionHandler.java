package cl.duoc.pedidos360.bff.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SERVICIO_CAIDO = "Servicio no disponible";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validación fallida en {}: {}", request.getRequestURI(), errores);
        return build(HttpStatus.BAD_REQUEST,
                ErrorBody.of(HttpStatus.BAD_REQUEST, "Error de validación", errores, request.getRequestURI()));
    }

    @ExceptionHandler(ValidacionDownstreamException.class)
    public ResponseEntity<Map<String, Object>> handleValidationDownstream(ValidacionDownstreamException ex,
            HttpServletRequest request) {
        log.warn("Validación rechazada por microservicio: {}", ex.getDetalles());
        return build(HttpStatus.BAD_REQUEST,
                ErrorBody.of(HttpStatus.BAD_REQUEST, "Error de validación", ex.getDetalles(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        log.warn("Cuerpo ilegible en {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ErrorBody.of(HttpStatus.BAD_REQUEST,
                "Cuerpo de la solicitud ausente o con formato inválido", request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorBody.of(HttpStatus.BAD_REQUEST,
                "Parámetro inválido: " + ex.getName(), request.getRequestURI()));
    }

    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ProductoNoEncontradoException ex,
            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND,
                ErrorBody.of(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResource(NoResourceFoundException ex,
            HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND,
                ErrorBody.of(HttpStatus.NOT_FOUND, "Recurso no encontrado", request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED,
                ErrorBody.of(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido", request.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaType(HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorBody.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Tipo de contenido no soportado", request.getRequestURI()));
    }

    @ExceptionHandler(AccesoRechazadoDownstreamException.class)
    public ResponseEntity<Map<String, Object>> handleAccesoRechazado(AccesoRechazadoDownstreamException ex,
            HttpServletRequest request) {
        log.warn("Microservicio respondió {}: {}", ex.getStatus().value(), ex.getMessage());
        return build(ex.getStatus(), ErrorBody.of(ex.getStatus(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleServicioNoDisponible(ServicioNoDisponibleException ex,
            HttpServletRequest request) {
        log.error("{} ({})", ex.getMessage(), ex.getStatus().value(), ex.getCause());
        return build(ex.getStatus(), ErrorBody.of(ex.getStatus(), ex.getMessage(), request.getRequestURI()));
    }

    /** Respuesta ilegible o error de cliente HTTP no clasificado: el detalle solo va al log. */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleRestClient(RestClientException ex,
            HttpServletRequest request) {
        log.error("Error al invocar microservicio", ex);
        return build(HttpStatus.BAD_GATEWAY,
                ErrorBody.of(HttpStatus.BAD_GATEWAY, SERVICIO_CAIDO, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorBody.of(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor", request.getRequestURI()));
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status).body(body);
    }
}
