package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.FakeHttpServer;
import cl.duoc.pedidos360.bff.support.TestJwt;

/**
 * Reenvío a report y audit: ruta, query params tal cual (también repetidos y con "+"), cabeceras (JWT,
 * X-Request-Id, X-Forwarded-For, X-Original-User-Agent), respuesta JSON sin tocar y errores downstream.
 */
class ReportAuditEndpointsTest extends AbstractBffTest {

    private static final String KPIS = "{\"rango\":{\"nombre\":\"today\"},\"ventas\":{\"monto\":4500.00,"
            + "\"cantidadPedidos\":2},\"estadosActivos\":{\"CREADO\":1},\"leadTime\":{\"p50Min\":null}}";
    private static final String TIMELINE = "[{\"eventId\":\"e1\",\"accion\":\"ORDER_CREATED\","
            + "\"donde\":{\"ip\":\"203.0.113.10\"}},{\"eventId\":\"e2\",\"accion\":\"ORDER_STATUS_CHANGED\"}]";

    @Autowired
    MockMvc mvc;

    private String bearer;

    @BeforeEach
    void setUp() {
        bearer = TestJwt.valid().roles("Admin").bearer();
        REPORT.respond(200, KPIS);
        AUDIT.respond(200, TIMELINE);
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, bearer).header("X-Request-Id", "req-e2e")
                .header("X-Forwarded-For", "203.0.113.10").header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (test)")
                .with(r -> {
                    r.setRemoteAddr("10.0.1.20");
                    return r;
                });
    }

    private void cabecerasPropagadas(FakeHttpServer server) {
        assertThat(server.last().authorization()).isEqualTo(bearer);
        assertThat(server.last().requestId()).isEqualTo("req-e2e");
        assertThat(server.last().forwardedFor()).isEqualTo("203.0.113.10, 10.0.1.20");
        assertThat(server.last().originalUserAgent()).isEqualTo("Mozilla/5.0 (test)");
    }

    private static String query(FakeHttpServer server) {
        return URLDecoder.decode(server.last().query(), StandardCharsets.UTF_8);
    }

    // ---------- report ----------

    @Test
    void kpisReenviaQueryYDevuelveElJsonTalCual() throws Exception {
        mvc.perform(auth(get("/api/report/kpis?range=today")))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-e2e"))
                .andExpect(jsonPath("$.ventas.monto").value(4500.00))
                .andExpect(jsonPath("$.estadosActivos.CREADO").value(1))
                .andExpect(jsonPath("$.leadTime.p50Min").isEmpty());
        assertThat(REPORT.last().method()).isEqualTo("GET");
        assertThat(REPORT.last().path()).isEqualTo("/api/report/kpis");
        assertThat(REPORT.last().query()).isEqualTo("range=today");
        cabecerasPropagadas(REPORT);
    }

    @Test
    void cadaEndpointDeReportVaASuRuta() throws Exception {
        mvc.perform(auth(get("/api/report/sales-by-hour?range=last7d"))).andExpect(status().isOk());
        assertThat(REPORT.last().path()).isEqualTo("/api/report/sales-by-hour");
        mvc.perform(auth(get("/api/report/lead-time"))).andExpect(status().isOk());
        assertThat(REPORT.last().path()).isEqualTo("/api/report/lead-time");
        assertThat(REPORT.last().query()).isNull();
        mvc.perform(auth(get("/api/report/top-products?range=last7d&limit=5&orden=monto"))).andExpect(status().isOk());
        assertThat(REPORT.last().path()).isEqualTo("/api/report/top-products");
        assertThat(REPORT.last().query()).isEqualTo("range=last7d&limit=5&orden=monto");
        mvc.perform(auth(get("/api/report/active-states"))).andExpect(status().isOk());
        assertThat(REPORT.last().path()).isEqualTo("/api/report/active-states");
        cabecerasPropagadas(REPORT);
    }

    @Test
    void fechasConOffsetSeCodificanCompletas() throws Exception {
        mvc.perform(auth(get("/api/report/kpis").param("desde", "2026-11-10T00:00:00+02:00")
                .param("hasta", "2026-11-10T23:59:59-03:00"))).andExpect(status().isOk());
        assertThat(REPORT.last().query()).contains("%2B").doesNotContain("+"); // sin codificar, report leería un espacio
        assertThat(query(REPORT)).isEqualTo("desde=2026-11-10T00:00:00+02:00&hasta=2026-11-10T23:59:59-03:00");
    }

    @Test
    void unRangeInvalidoDeReportLlegaComo400ConDetalles() throws Exception {
        REPORT.respond(400, "{\"status\":400,\"mensaje\":\"Error de validación\",\"detalles\":{\"range\":"
                + "\"Valor no permitido: 'ayer'. Valores permitidos: last24h, last7d, last30d, today\"}}");
        mvc.perform(auth(get("/api/report/kpis?range=ayer")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.range").value(org.hamcrest.Matchers.containsString("last24h")));
    }

    @Test
    void unaEscrituraNoExisteEnElBff() throws Exception {
        mvc.perform(auth(post("/api/report/kpis")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // ---------- audit ----------

    @Test
    void timelineDePedidoReenviaRutaYCabeceras() throws Exception {
        mvc.perform(auth(get("/api/audit/orders/42/timeline")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].donde.ip").value("203.0.113.10"));
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/orders/42/timeline");
        cabecerasPropagadas(AUDIT);
    }

    @Test
    void eventosReenviaFiltrosRepetidos() throws Exception {
        AUDIT.respond(200, "{\"contenido\":[],\"totalElementos\":0}");
        mvc.perform(auth(get("/api/audit/events?usuario=Juan Soto&accion=ORDER_CREATED&accion=ORDER_DELETED&size=5")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/events");
        assertThat(query(AUDIT)).isEqualTo("usuario=Juan Soto&accion=ORDER_CREATED&accion=ORDER_DELETED&size=5");
    }

    @Test
    void cadaEndpointDeAuditVaASuRuta() throws Exception {
        AUDIT.respond(200, "{}");
        mvc.perform(auth(get("/api/audit/events/abc-123"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/events/abc-123");
        mvc.perform(auth(get("/api/audit/actors/oid-1/timeline?desde=2026-11-01"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/actors/oid-1/timeline");
        assertThat(AUDIT.last().query()).isEqualTo("desde=2026-11-01");
        AUDIT.respond(200, "[]");
        mvc.perform(auth(get("/api/audit/event-types"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/event-types");
        AUDIT.respond(200, "{}");
        mvc.perform(auth(get("/api/audit/summary"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/summary");
        mvc.perform(auth(get("/api/audit/dead-letters?topic=audit.timeline"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/dead-letters");
        mvc.perform(auth(get("/api/audit/dead-letters/7"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/dead-letters/7");
        cabecerasPropagadas(AUDIT);
    }

    @Test
    void unIdConCaracteresEspecialesSeCodifica() throws Exception {
        AUDIT.respond(200, "{}");
        mvc.perform(auth(get("/api/audit/actors/{id}/timeline", "a b"))).andExpect(status().isOk());
        assertThat(AUDIT.last().path()).isEqualTo("/api/audit/actors/a b/timeline");
    }

    @Test
    void erroresDeAuditSeMapean() throws Exception {
        AUDIT.respond(404, "{\"status\":404,\"mensaje\":\"No hay eventos de auditoría para el pedido 999\"}");
        mvc.perform(auth(get("/api/audit/orders/999/timeline")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("No hay eventos de auditoría para el pedido 999"));
        AUDIT.respond(403, "{}");
        mvc.perform(auth(get("/api/audit/events"))).andExpect(status().isForbidden());
        AUDIT.respond(401, "{}");
        mvc.perform(auth(get("/api/audit/events"))).andExpect(status().isUnauthorized());
        AUDIT.respond(503, "{\"status\":503,\"mensaje\":\"Base de datos no disponible\"}");
        mvc.perform(auth(get("/api/audit/events"))).andExpect(status().isServiceUnavailable());
        AUDIT.respond(500, "{}");
        mvc.perform(auth(get("/api/audit/events"))).andExpect(status().isBadGateway());
    }
}
