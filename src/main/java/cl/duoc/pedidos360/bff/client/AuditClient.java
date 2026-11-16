package cl.duoc.pedidos360.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

/**
 * Cliente de ms-pedidos360-audit (timeline de auditoría y DLT, solo GET). Las respuestas pasan como JSON genérico.
 * 404 (evento, timeline o mensaje fallido inexistente), 409 y 503 se propagan con el mensaje de audit
 * ({@link DownstreamRestClientFactory#PROPAGAR_NEGOCIO}); 400 con sus detalles; 401/403 igual; caído → 503.
 */
@Component
public class AuditClient {

    private static final String AUDIT = "/api/audit";

    private final RestClient restClient;

    public AuditClient(DownstreamRestClientFactory factory,
            @Value("${pedidos360.services.audit.base-url}") String baseUrl) {
        this.restClient = factory.create("auditoría", baseUrl, DownstreamRestClientFactory.PROPAGAR_NEGOCIO);
    }

    public JsonNode eventos(MultiValueMap<String, String> params) {
        return get("/events", params);
    }

    public JsonNode evento(String eventId) {
        return get("/events/{v0}", null, eventId);
    }

    public JsonNode timelinePedido(String pedidoId) {
        return get("/orders/{v0}/timeline", null, pedidoId);
    }

    public JsonNode timelineActor(String actorId, MultiValueMap<String, String> params) {
        return get("/actors/{v0}/timeline", params, actorId);
    }

    public JsonNode tiposDeEvento() {
        return get("/event-types", null);
    }

    public JsonNode resumen(MultiValueMap<String, String> params) {
        return get("/summary", params);
    }

    public JsonNode mensajesFallidos(MultiValueMap<String, String> params) {
        return get("/dead-letters", params);
    }

    public JsonNode mensajeFallido(Long id) {
        return get("/dead-letters/{v0}", null, id);
    }

    /** Las variables de ruta van como variables de plantilla (codificadas), nunca concatenadas. */
    private JsonNode get(String ruta, MultiValueMap<String, String> params, Object... variables) {
        return restClient.get()
                .uri(builder -> QueryParams.conQuery(builder.path(AUDIT + ruta), params, variables))
                .retrieve()
                .body(JsonNode.class);
    }
}
