package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.TestJwt;

class SecurityTokenTest extends AbstractBffTest {

    private static final String URL = "/api/catalog/products";

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        CATALOG.respond(200, "[]");
    }

    private ResultActions getWith(TestJwt jwt) throws Exception {
        return mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, jwt.bearer()));
    }

    @Test
    void sinToken401ConCuerpoJson() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.path").value(URL))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void tokenBasuraDa401() throws Exception {
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer no.es.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenValidoConScopePasa() throws Exception {
        getWith(TestJwt.valid()).andExpect(status().isOk());
    }

    @Test
    void tokenSinScopeDa403() throws Exception {
        getWith(TestJwt.valid().scope(null))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.path").value(URL));
    }

    @Test
    void audienceIncorrectaDa401() throws Exception {
        getWith(TestJwt.valid().audience("otra-api")).andExpect(status().isUnauthorized());
    }

    @Test
    void issuerIncorrectoDa401() throws Exception {
        getWith(TestJwt.valid().issuer("https://evil.example.com/v2.0")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoDa401() throws Exception {
        getWith(TestJwt.valid().expiresAt(Instant.now().minusSeconds(600))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAunNoVigenteDa401() throws Exception {
        getWith(TestJwt.valid().notBefore(Instant.now().plusSeconds(600))).andExpect(status().isUnauthorized());
    }

    @Test
    void firmaInvalidaDa401() throws Exception {
        getWith(TestJwt.valid().signedWithOtherKey()).andExpect(status().isUnauthorized());
    }

    @Test
    void healthEsPublicoYApiDocs() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void otrosEndpointsSinTokenDan401() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/otro")).andExpect(status().isUnauthorized());
    }
}
