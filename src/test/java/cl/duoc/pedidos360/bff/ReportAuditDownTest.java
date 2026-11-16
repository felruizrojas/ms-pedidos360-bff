package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.support.TestJwt;
import cl.duoc.pedidos360.bff.support.TestJwtDecoderConfig;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + TestJwt.ISSUER,
        "spring.security.oauth2.resourceserver.jwt.audiences=" + TestJwt.AUDIENCE,
        "pedidos360.services.report.base-url=http://127.0.0.1:1",
        "pedidos360.services.audit.base-url=http://127.0.0.1:1"
})
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
class ReportAuditDownTest {

    @Autowired
    MockMvc mvc;

    @Test
    void reportCaidoDa503() throws Exception {
        mvc.perform(get("/api/report/kpis").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de reportes no disponible"));
    }

    @Test
    void auditCaidoDa503() throws Exception {
        mvc.perform(get("/api/audit/events").header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de auditoría no disponible"));
    }
}
