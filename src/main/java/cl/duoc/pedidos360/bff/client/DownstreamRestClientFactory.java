package cl.duoc.pedidos360.bff.client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
import cl.duoc.pedidos360.bff.exception.ServicioNoDisponibleException;
import cl.duoc.pedidos360.bff.exception.ValidacionDownstreamException;
import cl.duoc.pedidos360.bff.web.RequestIdFilter;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Crea los RestClient de todos los microservicios con lo común: timeouts, propagación de Authorization y
 * X-Request-Id, y traducción de errores. Un microservicio nuevo solo llama a {@link #create}.
 * Un 404 no se traduce aquí: cada client lo convierte a su excepción de dominio con {@code onStatus}.
 */
@Slf4j
@Component
public class DownstreamRestClientFactory {

    private final DownstreamProperties properties;
    private final ObjectMapper objectMapper;

    public DownstreamRestClientFactory(DownstreamProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** @param servicio nombre legible para mensajes de error, p. ej. "catálogo". */
    public RestClient create(String servicio, String baseUrl) {
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
                            throw traducir(servicio, response);
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

    private RuntimeException traducir(String servicio, ClientHttpResponse response) throws IOException {
        HttpStatusCode status = response.getStatusCode();
        int code = status.value();
        if (code == 400) {
            return validacion(response);
        }
        if (code == 401 || code == 403) {
            return new AccesoRechazadoDownstreamException(HttpStatus.valueOf(code), servicio);
        }
        // 5xx u otro código inesperado (incluye 404 en operaciones donde no tiene sentido)
        log.error("Servicio de {} respondió {}", servicio, code);
        return new ServicioNoDisponibleException(HttpStatus.BAD_GATEWAY, servicio, null);
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
