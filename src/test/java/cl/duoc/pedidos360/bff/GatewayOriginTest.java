package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.TestJwt;
import cl.duoc.pedidos360.bff.web.GatewayOriginFilter;

@TestPropertySource(properties = "pedidos360.gateway.secret=s3creto")
class GatewayOriginTest extends AbstractBffTest {

    private static final String URL = "/api/catalog/products";

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        CATALOG.respond(200, "[]");
    }

    @Test
    void tokenValidoSinHeaderDelGatewayDa403() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Acceso permitido solo a través del API Gateway"));
    }

    @Test
    void headerIncorrectoDa403() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer())
                .header(GatewayOriginFilter.HEADER, "otro"))
                .andExpect(status().isForbidden());
    }

    @Test
    void headerCorrectoYTokenValidoDa200() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer())
                .header(GatewayOriginFilter.HEADER, "s3creto"))
                .andExpect(status().isOk());
    }

    @Test
    void headerCorrectoSinTokenSigueDando401() throws Exception {
        mvc.perform(get(URL).header(GatewayOriginFilter.HEADER, "s3creto"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthYSwaggerNoExigenHeader() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
