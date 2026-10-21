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
    void endpointsDePedidosDocumentadosConBearerAuthYErrores() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].get.responses['200'].content['*/*'].schema").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].delete.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}/status'].put.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}/history'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/orders/{id}'].put.responses['503']").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].get.responses['502']").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/orders'].post.responses['403']").exists());
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
