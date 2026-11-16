package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.PedidosEjemplo;
import cl.duoc.pedidos360.bff.support.TestJwt;

/**
 * El BFF reenvía a los microservicios desde dónde vino la petición del usuario (para audit.timeline):
 * X-Forwarded-For (recibido + IP remota) y X-Original-User-Agent (el User-Agent del navegador).
 */
class OrigenPeticionHeadersTest extends AbstractBffTest {

    private static final String NAVEGADOR = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_0) Firefox/140.0";

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        ORDERS.respond(200, PedidosEjemplo.PEDIDO);
        CATALOG.respond(200, "[]");
    }

    private MockHttpServletRequestBuilder desde(MockHttpServletRequestBuilder req, String ipRemota) {
        return req.header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer())
                .header(HttpHeaders.USER_AGENT, NAVEGADOR)
                .with(r -> {
                    r.setRemoteAddr(ipRemota);
                    return r;
                });
    }

    @Test
    void conXForwardedForEntranteAgregaLaIpRemota() throws Exception {
        // El API Gateway ya agregó la IP del usuario; el BFF ve como remota la del Gateway.
        mvc.perform(desde(get("/api/orders/15"), "10.0.1.20").header("X-Forwarded-For", "203.0.113.10"))
                .andExpect(status().isOk());

        assertThat(ORDERS.last().forwardedFor()).isEqualTo("203.0.113.10, 10.0.1.20");
        assertThat(ORDERS.last().originalUserAgent()).isEqualTo(NAVEGADOR);
    }

    @Test
    void sinXForwardedForEntranteEnviaSoloLaIpRemota() throws Exception {
        mvc.perform(desde(get("/api/orders/15"), "198.51.100.7")).andExpect(status().isOk());

        assertThat(ORDERS.last().forwardedFor()).isEqualTo("198.51.100.7");
        assertThat(ORDERS.last().originalUserAgent()).isEqualTo(NAVEGADOR);
    }

    @Test
    void conVariosValoresLosConservaEnOrden() throws Exception {
        mvc.perform(desde(get("/api/orders/15"), "10.0.1.20")
                        .header("X-Forwarded-For", "203.0.113.10, 172.16.0.4"))
                .andExpect(status().isOk());

        assertThat(ORDERS.last().forwardedFor()).isEqualTo("203.0.113.10, 172.16.0.4, 10.0.1.20");
    }

    @Test
    void tambienLosRecibeCatalog() throws Exception {
        // Mismo interceptor para todos los clientes downstream.
        mvc.perform(desde(get("/api/catalog/products"), "10.0.1.20").header("X-Forwarded-For", "203.0.113.10"))
                .andExpect(status().isOk());

        assertThat(CATALOG.last().forwardedFor()).isEqualTo("203.0.113.10, 10.0.1.20");
        assertThat(CATALOG.last().originalUserAgent()).isEqualTo(NAVEGADOR);
    }
}
