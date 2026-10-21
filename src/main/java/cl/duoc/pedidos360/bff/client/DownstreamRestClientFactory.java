package cl.duoc.pedidos360.bff.client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.pedidos360.bff.config.DownstreamProperties;
import cl.duoc.pedidos360.bff.exception.AccesoRechazadoDownstreamException;
import cl.duoc.pedidos360.bff.exception.ConflictoDownstreamException;
import cl.duoc.pedidos360.bff.exception.NoEncontradoDownstreamException;
import cl.duoc.pedidos360.bff.exception.ServicioNoDisponibleException;
import cl.duoc.pedidos360.bff.exception.ValidacionDownstreamException;
import cl.duoc.pedidos360.bff.web.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Crea los RestClient de todos los microservicios con lo común: timeouts, propagación de Authorization y
 * X-Request-Id, y traducción de errores. Un microservicio nuevo solo llama a {@link #create}.
 * <ul>
 * <li>Siempre: 400 → 400 con detalles · 401/403 → igual · otro código → 502 · caído/timeout → 503.</li>
 * <li>Solo los códigos que el client pide propagar ({@link #PROPAGAR_NEGOCIO}): 404 y 409 con el mensaje
 * (y detalles) del microservicio, 503 como 503. Catalog no los pide: su 404 lo resuelve con
 * {@code onStatus} y su 409/503 siguen siendo 502.</li>
 * </ul>
 */
@Slf4j
@Component
public class DownstreamRestClientFactory {

    /** 404, 409 y 503 de negocio que se reenvían al front tal cual (los usa orders). */
    public static final Set<HttpStatus> PROPAGAR_NEGOCIO =
            Set.of(HttpStatus.NOT_FOUND, HttpStatus.CONFLICT, HttpStatus.SERVICE_UNAVAILABLE);

    private final DownstreamProperties properties;
    private final ObjectMapper objectMapper;

    public DownstreamRestClientFactory(DownstreamProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** @param servicio nombre legible para mensajes de error, p. ej. "catálogo". */
    public RestClient create(String servicio, String baseUrl) {
        return create(servicio, baseUrl, Set.of());
    }

    /** @param propagar códigos (de {@link #PROPAGAR_NEGOCIO}) que se reenvían en vez de convertirse en 502. */
    public RestClient create(String servicio, String baseUrl, Set<HttpStatus> propagar) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    propagarCabeceras(request);
                    try {
                        return execution.execute(request, body);
                    } catch (IOException e) {
                        // conexión rechazada, timeout, DNS, etc.: sin detalles hacia el front
                        throw new ServicioNoDisponibleException(HttpStatus.SERVICE_UNAVAILABLE, servicio, e);
                    }
                })
                .defaultStatusHandler(HttpStatusCode::isError,
                        (request, response) -> {
                            throw traducir(servicio, response, propagar);
                        })
                .build();
    }

    private void propagarCabeceras(HttpRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
        }
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            request.getHeaders().set(RequestIdFilter.HEADER, requestId);
        }
    }

    private RuntimeException traducir(String servicio, ClientHttpResponse response, Set<HttpStatus> propagar)
            throws IOException {
        HttpStatusCode status = response.getStatusCode();
        int code = status.value();
        if (code == 400) {
            return validacion(response);
        }
        if (code == 401 || code == 403) {
            return new AccesoRechazadoDownstreamException(HttpStatus.valueOf(code), servicio);
        }
        HttpStatus conocido = HttpStatus.resolve(code);
        if (conocido != null && propagar.contains(conocido)) {
            Map<String, Object> body = leerCuerpo(response);
            String mensaje = body.get("mensaje") instanceof String m ? m : null;
            return switch (conocido) {
                case NOT_FOUND -> new NoEncontradoDownstreamException(
                        mensaje != null ? mensaje : "Recurso no encontrado en el servicio de " + servicio);
                case CONFLICT -> new ConflictoDownstreamException(
                        mensaje != null ? mensaje : "Conflicto en el servicio de " + servicio, detalles(body));
                default -> {
                    log.error("Servicio de {} respondió {}: {}", servicio, code, mensaje);
                    yield mensaje != null
                            ? ServicioNoDisponibleException.conMensaje(HttpStatus.SERVICE_UNAVAILABLE, mensaje)
                            : new ServicioNoDisponibleException(HttpStatus.SERVICE_UNAVAILABLE, servicio, null);
                }
            };
        }
        // 5xx u otro código inesperado (incluye 404 en operaciones donde no tiene sentido)
        log.error("Servicio de {} respondió {}", servicio, code);
        return new ServicioNoDisponibleException(HttpStatus.BAD_GATEWAY, servicio, null);
    }

    /** Cuerpo de error del microservicio como mapa; vacío si no es JSON. */
    private Map<String, Object> leerCuerpo(ClientHttpResponse response) {
        try {
            Map<String, Object> body = objectMapper.readValue(response.getBody(), new TypeReference<>() { });
            return body != null ? body : Map.of();
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo leer el cuerpo del error del microservicio: {}", e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> detalles(Map<String, Object> body) {
        return body.get("detalles") instanceof Map<?, ?> d ? new LinkedHashMap<>((Map<String, Object>) d) : null;
    }

    private ValidacionDownstreamException validacion(ClientHttpResponse response) {
        Map<String, String> detalles = Collections.emptyMap();
        String mensaje = "Error de validación";
        try {
            Map<String, Object> body = objectMapper.readValue(response.getBody(), new TypeReference<>() { });
            if (body.get("detalles") instanceof Map<?, ?> d) {
                Map<String, String> copia = new LinkedHashMap<>();
                d.forEach((k, v) -> copia.put(String.valueOf(k), String.valueOf(v)));
                detalles = copia;
            }
            if (body.get("mensaje") instanceof String m) {
                mensaje = m;
            }
        } catch (IOException | RuntimeException e) {
            log.warn("No se pudo leer el detalle del 400 del microservicio: {}", e.getMessage());
        }
        return new ValidacionDownstreamException(mensaje, detalles);
    }
}
