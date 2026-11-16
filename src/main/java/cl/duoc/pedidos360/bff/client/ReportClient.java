package cl.duoc.pedidos360.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

/**
 * Cliente de ms-pedidos360-report (KPIs, solo GET). Las respuestas pasan como JSON genérico: el BFF no duplica los
 * DTOs de report. 404, 409 y 503 se propagan con el mensaje de report ({@link DownstreamRestClientFactory#PROPAGAR_NEGOCIO});
 * 400 con sus detalles (p. ej. range inválido); 401/403 igual; caído → 503.
 */
@Component
public class ReportClient {

    private static final String REPORT = "/api/report";

    private final RestClient restClient;

    public ReportClient(DownstreamRestClientFactory factory,
            @Value("${pedidos360.services.report.base-url}") String baseUrl) {
        this.restClient = factory.create("reportes", baseUrl, DownstreamRestClientFactory.PROPAGAR_NEGOCIO);
    }

    public JsonNode kpis(MultiValueMap<String, String> params) {
        return get("/kpis", params);
    }

    public JsonNode ventasPorHora(MultiValueMap<String, String> params) {
        return get("/sales-by-hour", params);
    }

    public JsonNode leadTime(MultiValueMap<String, String> params) {
        return get("/lead-time", params);
    }

    public JsonNode topProductos(MultiValueMap<String, String> params) {
        return get("/top-products", params);
    }

    public JsonNode estadosActivos() {
        return get("/active-states", null);
    }

    private JsonNode get(String ruta, MultiValueMap<String, String> params) {
        return restClient.get()
                .uri(builder -> QueryParams.conQuery(builder.path(REPORT + ruta), params))
                .retrieve()
                .body(JsonNode.class);
    }
}
