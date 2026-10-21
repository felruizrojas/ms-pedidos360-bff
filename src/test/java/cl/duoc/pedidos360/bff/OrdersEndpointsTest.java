package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import cl.duoc.pedidos360.bff.support.PedidosEjemplo;
import cl.duoc.pedidos360.bff.support.TestJwt;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class OrdersEndpointsTest extends AbstractBffTest {

    private static final String URL = "/api/orders";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    MockMvc mvc;

    private String bearer;

    @BeforeEach
    void setUp() {
        ORDERS.respond(200, PedidosEjemplo.PEDIDO);
        bearer = TestJwt.valid().bearer();
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, bearer).header("X-Request-Id", "req-orders");
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return auth(req).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Authorization y X-Request-Id llegan a orders tal cual. */
    private void cabecerasPropagadas() {
        assertThat(ORDERS.last().authorization()).isEqualTo(bearer);
        assertThat(ORDERS.last().requestId()).isEqualTo("req-orders");
    }

    @Test
    void crearReenviaPostConBodyYDevuelve201() throws Exception {
        ORDERS.respond(201, PedidosEjemplo.PEDIDO.replace("\"ACEPTADO\"", "\"CREADO\""));
        mvc.perform(json(post(URL), PedidosEjemplo.CREAR))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Request-Id", "req-orders"))
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.estado").value("CREADO"))
                .andExpect(jsonPath("$.total").value(2700.00))
                .andExpect(jsonPath("$.stockDescontado").value(true))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[1].productoNombre").value("Café Americano"))
                .andExpect(jsonPath("$.items[1].subtotal").value(1500.00))
                .andExpect(jsonPath("$.creadoEn").value("2026-10-20T15:04:05.123Z"))
                .andExpect(jsonPath("$.version").value(1));

        assertThat(ORDERS.last().method()).isEqualTo("POST");
        assertThat(ORDERS.last().path()).isEqualTo(URL);
        assertThat(ORDERS.last().contentType()).startsWith("application/json");
        assertThat(JSON.readTree(ORDERS.last().body())).isEqualTo(JSON.readTree(PedidosEjemplo.CREAR));
        cabecerasPropagadas();
    }

    @Test
    void obtenerReenviaGetConId() throws Exception {
        mvc.perform(auth(get(URL + "/15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.aceptadoEn").value("2026-10-20T15:10:00.456Z"))
                .andExpect(jsonPath("$.despachadoEn").doesNotExist());
        assertThat(ORDERS.last().method()).isEqualTo("GET");
        assertThat(ORDERS.last().path()).isEqualTo(URL + "/15");
        cabecerasPropagadas();
    }

    @Test
    void listarReenviaLosQueryParamsTalCual() throws Exception {
        ORDERS.respond(200, PedidosEjemplo.PAGINA);
        mvc.perform(auth(get(URL)
                        .param("estado", "CREADO")
                        .param("desde", "2026-10-01T00:00:00+02:00")
                        .param("hasta", "2026-10-31")
                        .param("page", "1")
                        .param("size", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(15))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));

        assertThat(ORDERS.last().path()).isEqualTo(URL);
        // el "+" del offset va codificado (%2B): sin codificar, orders lo leería como espacio
        assertThat(ORDERS.last().query()).contains("%2B").doesNotContain("+");
        assertThat(URLDecoder.decode(ORDERS.last().query(), StandardCharsets.UTF_8))
                .isEqualTo("estado=CREADO&desde=2026-10-01T00:00:00+02:00&hasta=2026-10-31&page=1&size=5");
        cabecerasPropagadas();
    }

    @Test
    void listarSinFiltrosNoAgregaQuery() throws Exception {
        ORDERS.respond(200, "{\"contenido\":[],\"pagina\":0,\"tamano\":20,\"totalElementos\":0,\"totalPaginas\":0}");
        mvc.perform(auth(get(URL))).andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(0));
        assertThat(ORDERS.last().query()).isNull();
    }

    @Test
    void listarConEstadoDesconocidoLoDejaValidarAOrders() throws Exception {
        ORDERS.respond(400, "{\"status\":400,\"mensaje\":\"Error de validación\","
                + "\"detalles\":{\"status\":\"Estado inválido: \\\"VOLANDO\\\"\"}}");
        mvc.perform(auth(get(URL).param("estado", "VOLANDO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.status").value("Estado inválido: \"VOLANDO\""));
        assertThat(ORDERS.last().query()).isEqualTo("estado=VOLANDO");
    }

    @Test
    void actualizarReenviaPutYSoloLosCamposQueVinieron() throws Exception {
        String body = "{\"items\":[{\"productoId\":4,\"cantidad\":3}]}";
        mvc.perform(json(put(URL + "/15"), body)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(15));

        assertThat(ORDERS.last().method()).isEqualTo("PUT");
        assertThat(ORDERS.last().path()).isEqualTo(URL + "/15");
        JsonNode enviado = JSON.readTree(ORDERS.last().body());
        assertThat(enviado).isEqualTo(JSON.readTree(body));
        assertThat(enviado.has("direccionEntrega")).isFalse();
        cabecerasPropagadas();
    }

    @Test
    void cambiarEstadoReenviaPutAStatus() throws Exception {
        String body = "{\"status\":\"CANCELADO\",\"comentario\":\"Cliente no estaba en el domicilio\"}";
        mvc.perform(json(put(URL + "/15/status"), body)).andExpect(status().isOk());

        assertThat(ORDERS.last().method()).isEqualTo("PUT");
        assertThat(ORDERS.last().path()).isEqualTo(URL + "/15/status");
        assertThat(JSON.readTree(ORDERS.last().body())).isEqualTo(JSON.readTree(body));
        cabecerasPropagadas();
    }

    @Test
    void historialReenviaGet() throws Exception {
        ORDERS.respond(200, PedidosEjemplo.HISTORIAL);
        mvc.perform(auth(get(URL + "/15/history")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$[1].estadoNuevo").value("ACEPTADO"))
                .andExpect(jsonPath("$[1].usuarioRol").value("Operador"))
                .andExpect(jsonPath("$[1].fecha").value("2026-10-20T15:10:00.456Z"));
        assertThat(ORDERS.last().path()).isEqualTo(URL + "/15/history");
        cabecerasPropagadas();
    }

    @Test
    void eliminarReenviaDeleteYDevuelve204() throws Exception {
        ORDERS.respond(204, "");
        mvc.perform(auth(delete(URL + "/15"))).andExpect(status().isNoContent());
        assertThat(ORDERS.last().method()).isEqualTo("DELETE");
        assertThat(ORDERS.last().path()).isEqualTo(URL + "/15");
        cabecerasPropagadas();
    }

    // --- Traducción de errores ---

    @Test
    void noEncontradoEnOrdersDa404ConSuMensaje() throws Exception {
        ORDERS.respond(404, "{\"status\":404,\"error\":\"Not Found\",\"mensaje\":\"Pedido 99 no encontrado\"}");
        mvc.perform(auth(get(URL + "/99")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Pedido 99 no encontrado"))
                .andExpect(jsonPath("$.path").value(URL + "/99"));
    }

    @Test
    void transicionInvalidaDa409ConMensajeYDetalles() throws Exception {
        ORDERS.respond(409, """
                {"timestamp":"2026-10-20T15:04:05.123Z","status":409,"error":"Conflict",
                 "mensaje":"No se puede despachar un pedido que no fue aceptado (estado actual: CREADO)",
                 "detalles":{"estadoActual":"CREADO","estadoSolicitado":"DESPACHADO"}}
                """);
        mvc.perform(json(put(URL + "/15/status"), "{\"status\":\"DESPACHADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede despachar un pedido que no fue aceptado (estado actual: CREADO)"))
                .andExpect(jsonPath("$.detalles.estadoActual").value("CREADO"))
                .andExpect(jsonPath("$.detalles.estadoSolicitado").value("DESPACHADO"))
                .andExpect(jsonPath("$.path").value(URL + "/15/status"));
    }

    @Test
    void sinStockDa409ConDetallesNumericos() throws Exception {
        ORDERS.respond(409, "{\"status\":409,\"mensaje\":\"Stock insuficiente para \\\"Pan Amasado\\\" (id 1)\","
                + "\"detalles\":{\"productoId\":1,\"solicitado\":5,\"disponible\":2}}");
        mvc.perform(json(put(URL + "/15/status"), "{\"status\":\"ACEPTADO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente para \"Pan Amasado\" (id 1)"))
                .andExpect(jsonPath("$.detalles.productoId").value(1))
                .andExpect(jsonPath("$.detalles.solicitado").value(5))
                .andExpect(jsonPath("$.detalles.disponible").value(2));
    }

    @Test
    void conflictoSinDetallesNoAgregaDetalles() throws Exception {
        ORDERS.respond(409, "{\"status\":409,\"mensaje\":\"El pedido fue modificado por otro usuario\"}");
        mvc.perform(json(put(URL + "/15"), "{\"direccionEntrega\":\"Los Leones 456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El pedido fue modificado por otro usuario"))
                .andExpect(jsonPath("$.detalles").doesNotExist());
    }

    @Test
    void error503DeOrdersSePropagaComo503() throws Exception {
        ORDERS.respond(503, "{\"status\":503,\"mensaje\":\"Servicio de catálogo no disponible\"}");
        mvc.perform(json(post(URL), PedidosEjemplo.CREAR))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de catálogo no disponible"));
    }

    @Test
    void otro5xxDeOrdersDa502SinFiltrarDetalles() throws Exception {
        ORDERS.respond(500, "{\"mensaje\":\"NullPointerException en PedidoService.java:42\"}");
        mvc.perform(auth(get(URL + "/15")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensaje").value("Servicio de pedidos no disponible"));
    }

    @Test
    void validacionDeOrdersDa400ConDetalles() throws Exception {
        ORDERS.respond(400, "{\"status\":400,\"mensaje\":\"Error de validación\","
                + "\"detalles\":{\"items[1].productoId\":\"El producto 99 no existe en el catálogo\"}}");
        mvc.perform(json(post(URL), PedidosEjemplo.CREAR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles['items[1].productoId']").value("El producto 99 no existe en el catálogo"));
    }

    @Test
    void ordersRechazaElRolDa403() throws Exception {
        ORDERS.respond(403, "{}");
        mvc.perform(auth(delete(URL + "/15"))).andExpect(status().isForbidden());
    }

    @Test
    void timeoutDeOrdersDa503() throws Exception {
        ORDERS.delay(3000);
        mvc.perform(auth(get(URL + "/15")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de pedidos no disponible"));
    }

    // --- Validación básica en el BFF (no llega a orders) ---

    @Test
    void crearSinDireccionNiItemsDa400SinLlamarAOrders() throws Exception {
        mvc.perform(json(post(URL), "{\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.direccionEntrega").value("La dirección de entrega es obligatoria."))
                .andExpect(jsonPath("$.detalles.items").value("El pedido debe tener al menos un ítem."));
        assertThat(ORDERS.last()).isNull();
    }

    @Test
    void itemSinCantidadDa400() throws Exception {
        mvc.perform(json(post(URL), "{\"direccionEntrega\":\"Av. Siempre Viva 742\",\"items\":[{\"productoId\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles['items[0].cantidad']").value("La cantidad es obligatoria."));
        assertThat(ORDERS.last()).isNull();
    }

    @Test
    void cambiarEstadoSinStatusDa400() throws Exception {
        mvc.perform(json(put(URL + "/15/status"), "{\"comentario\":\"hola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.status").value("El status es obligatorio."));
        assertThat(ORDERS.last()).isNull();
    }

    @Test
    void idInvalidoDa400() throws Exception {
        mvc.perform(auth(get(URL + "/abc"))).andExpect(status().isBadRequest());
        assertThat(ORDERS.last()).isNull();
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        assertThat(ORDERS.last()).isNull();
    }
}
