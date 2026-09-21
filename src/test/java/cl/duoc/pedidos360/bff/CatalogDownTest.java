package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.support.TestJwtDecoderConfig;
import cl.duoc.pedidos360.bff.support.TestJwt;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + TestJwt.ISSUER,
        "spring.security.oauth2.resourceserver.jwt.audiences=" + TestJwt.AUDIENCE,
        "pedidos360.services.catalog.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class CatalogDownTest {

    @Autowired
    MockMvc mvc;

    @Test
    void conexionRechazadaDa503() throws Exception {
        mvc.perform(get("/api/catalog/products").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de catálogo no disponible"))
                .andExpect(jsonPath("$.path").value("/api/catalog/products"));
    }
}
