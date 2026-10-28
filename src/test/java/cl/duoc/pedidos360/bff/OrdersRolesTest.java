package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.PedidosEjemplo;
import cl.duoc.pedidos360.bff.support.TestJwt;

/** ENFORCE_ROLES=true: el Cliente opera sus pedidos, no el catálogo; borrar pedidos es solo del Operador; Admin no opera pedidos. */
@TestPropertySource(properties = "pedidos360.security.enforce-roles=true")
class OrdersRolesTest extends AbstractBffTest {

    private static final String URL = "/api/orders";

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        ORDERS.respond(200, PedidosEjemplo.PEDIDO);
    }

    private ResultActions as(TestJwt jwt, MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header(HttpHeaders.AUTHORIZATION, jwt.bearer()));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String body) {
        return req.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static TestJwt cliente() {
        return TestJwt.valid().roles("Cliente");
    }

    @Test
    void clientePuedeCrearPedido() throws Exception {
        ORDERS.respond(201, PedidosEjemplo.PEDIDO);
        as(cliente(), json(post(URL), PedidosEjemplo.CREAR)).andExpect(status().isCreated());
    }

    @Test
    void clientePuedeEditarPedido() throws Exception {
        as(cliente(), json(put(URL + "/15"), "{\"direccionEntrega\":\"Los Leones 456\"}")).andExpect(status().isOk());
    }

    @Test
    void clientePuedeCancelarPedido() throws Exception {
        as(cliente(), json(put(URL + "/15/status"), "{\"status\":\"CANCELADO\"}")).andExpect(status().isOk());
    }

    @Test
    void clientePuedeListarYVerHistorial() throws Exception {
        ORDERS.respond(200, PedidosEjemplo.PAGINA);
        as(cliente(), get(URL)).andExpect(status().isOk());
        ORDERS.respond(200, PedidosEjemplo.HISTORIAL);
        as(cliente(), get(URL + "/15/history")).andExpect(status().isOk());
    }

    @Test
    void operadorPuedeCambiarEstado() throws Exception {
        as(TestJwt.valid().roles("Operador"), json(put(URL + "/15/status"), "{\"status\":\"ACEPTADO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        CATALOG.respond(201, "{\"id\":1,\"nombre\":\"Mouse\",\"precio\":1000,\"stock\":1}");
        as(cliente(), json(post("/api/catalog/products"), "{\"nombre\":\"Mouse\",\"precio\":1000,\"stock\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soloOperadorPuedeEliminarPedido() throws Exception {
        ORDERS.respond(204, "");
        as(TestJwt.valid().roles("Operador"), delete(URL + "/15")).andExpect(status().isNoContent());
    }

    @Test
    void adminNoPuedeEliminarPedido() throws Exception {
        as(TestJwt.valid().roles("Admin"), delete(URL + "/15")).andExpect(status().isForbidden());
    }

    @Test
    void adminNoOperaPedidos() throws Exception {
        as(TestJwt.valid().roles("Admin"), get(URL)).andExpect(status().isForbidden());
        as(TestJwt.valid().roles("Admin"), json(put(URL + "/15/status"), "{\"status\":\"ACEPTADO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoPuedeEliminarPedido() throws Exception {
        as(cliente(), delete(URL + "/15")).andExpect(status().isForbidden());
    }

    @Test
    void sinRolDa403EnPedidos() throws Exception {
        as(TestJwt.valid(), get(URL + "/15")).andExpect(status().isForbidden());
    }

    @Test
    void rolSinScopeDa403() throws Exception {
        as(TestJwt.valid().scope(null).roles("Cliente"), json(post(URL), PedidosEjemplo.CREAR))
                .andExpect(status().isForbidden());
    }
}
