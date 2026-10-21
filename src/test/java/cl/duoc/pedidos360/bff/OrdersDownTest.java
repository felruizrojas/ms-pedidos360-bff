package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.support.PedidosEjemplo;
import cl.duoc.pedidos360.bff.support.TestJwt;
import cl.duoc.pedidos360.bff.support.TestJwtDecoderConfig;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + TestJwt.ISSUER,
        "spring.security.oauth2.resourceserver.jwt.audiences=" + TestJwt.AUDIENCE,
        "pedidos360.services.orders.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class OrdersDownTest {

    @Autowired
    MockMvc mvc;

    @Test
    void conexionRechazadaDa503() throws Exception {
        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de pedidos no disponible"))
                .andExpect(jsonPath("$.path").value("/api/orders"));
    }

    @Test
    void crearConConexionRechazadaDa503() throws Exception {
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(PedidosEjemplo.CREAR))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de pedidos no disponible"));
    }
}
