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

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.TestJwt;

@TestPropertySource(properties = "pedidos360.security.enforce-roles=true")
class EnforceRolesTest extends AbstractBffTest {

    private static final String URL = "/api/catalog/products";
    private static final String NUEVO = "{\"nombre\":\"Mouse\",\"precio\":1000,\"stock\":1}";

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        CATALOG.respond(201, "{\"id\":1,\"nombre\":\"Mouse\",\"precio\":1000,\"stock\":1}");
    }

    private org.springframework.test.web.servlet.ResultActions postAs(TestJwt jwt) throws Exception {
        return mvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, jwt.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(NUEVO));
    }

    @Test
    void postSinRolDa403() throws Exception {
        postAs(TestJwt.valid()).andExpect(status().isForbidden());
    }

    @Test
    void postConRolClienteDa403() throws Exception {
        postAs(TestJwt.valid().roles("Cliente")).andExpect(status().isForbidden());
    }

    @Test
    void postConAdminDa201() throws Exception {
        postAs(TestJwt.valid().roles("Admin")).andExpect(status().isCreated());
    }

    @Test
    void postConOperadorDa403() throws Exception {
        postAs(TestJwt.valid().roles("Operador")).andExpect(status().isForbidden());
    }

    @Test
    void getSigueAbiertoACualquierAutenticadoConScope() throws Exception {
        CATALOG.respond(200, "[]");
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void rolSinScopeDa403() throws Exception {
        postAs(TestJwt.valid().scope(null).roles("Admin")).andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.ResultActions putAs(TestJwt jwt) throws Exception {
        return mvc.perform(put(URL + "/1").header(HttpHeaders.AUTHORIZATION, jwt.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"precio\":900,\"stock\":3}"));
    }

    @Test
    void putConRolClienteDa403() throws Exception {
        putAs(TestJwt.valid().roles("Cliente")).andExpect(status().isForbidden());
    }

    @Test
    void putConAdminDa200() throws Exception {
        CATALOG.respond(200, "{\"id\":1,\"nombre\":\"Mouse\",\"precio\":900,\"stock\":3}");
        putAs(TestJwt.valid().roles("Admin")).andExpect(status().isOk());
    }

    @Test
    void putConOperadorDa403() throws Exception {
        putAs(TestJwt.valid().roles("Operador")).andExpect(status().isForbidden());
    }

    @Test
    void deleteConRolClienteDa403() throws Exception {
        mvc.perform(delete(URL + "/1").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Cliente").bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteConAdminDa204() throws Exception {
        CATALOG.respond(204, "");
        mvc.perform(delete(URL + "/1").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Admin").bearer()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminadosConRolClienteDa403() throws Exception {
        mvc.perform(get(URL + "/eliminados").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Cliente").bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurarConRolClienteDa403() throws Exception {
        mvc.perform(put(URL + "/1/restaurar").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Cliente").bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void restaurarConAdminDa200() throws Exception {
        CATALOG.respond(200, "{\"id\":1,\"nombre\":\"Taza\",\"descripcion\":\"\",\"precio\":1000,\"stock\":1}");
        mvc.perform(put(URL + "/1/restaurar").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Admin").bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void restaurarConOperadorDa403() throws Exception {
        mvc.perform(put(URL + "/1/restaurar").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles("Operador").bearer()))
                .andExpect(status().isForbidden());
    }
}
