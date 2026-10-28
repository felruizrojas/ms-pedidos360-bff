package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.TestJwt;

/** Sin GRAPH_CLIENT_SECRET (configuración por defecto) la vista de usuarios responde 503 con un mensaje claro. */
class UsersSinGraphTest extends AbstractBffTest {

    @Autowired
    MockMvc mvc;

    @Test
    void sinSecretDa503() throws Exception {
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Admin").bearer()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Microsoft Graph no está configurado en el BFF (falta GRAPH_CLIENT_SECRET)"));
    }
}
