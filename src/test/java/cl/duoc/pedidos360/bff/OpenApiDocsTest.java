package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;

class OpenApiDocsTest extends AbstractBffTest {

    @Autowired
    MockMvc mvc;

    @Test
    void apiDocsSinTokenDevuelve200ConCampoOpenapi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void swaggerUiSinTokenResponde() throws Exception {
        int statusCode = mvc.perform(get("/swagger-ui/index.html"))
                .andReturn().getResponse().getStatus();
        assertThat(statusCode).satisfiesAnyOf(
                s -> assertThat(s).isEqualTo(200),
                s -> assertThat(s).isBetween(300, 399));
    }
}
