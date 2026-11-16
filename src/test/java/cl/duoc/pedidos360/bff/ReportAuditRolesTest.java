package cl.duoc.pedidos360.bff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.TestJwt;

/** ENFORCE_ROLES=true: report solo Admin; audit Admin o Auditor. */
@TestPropertySource(properties = "pedidos360.security.enforce-roles=true")
class ReportAuditRolesTest extends AbstractBffTest {

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        REPORT.respond(200, "{}");
        AUDIT.respond(200, "{}");
    }

    private ResultActions as(String url, String... roles) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, TestJwt.valid().roles(roles).bearer()));
    }

    @Test
    void reportSoloAdmin() throws Exception {
        as("/api/report/kpis", "Admin").andExpect(status().isOk());
        as("/api/report/kpis", "Auditor").andExpect(status().isForbidden());
        as("/api/report/top-products", "Operador").andExpect(status().isForbidden());
        as("/api/report/active-states", "Cliente").andExpect(status().isForbidden());
    }

    @Test
    void auditAdminOAuditor() throws Exception {
        as("/api/audit/events", "Admin").andExpect(status().isOk());
        as("/api/audit/events", "Auditor").andExpect(status().isOk());
        as("/api/audit/dead-letters", "Operador").andExpect(status().isForbidden());
        as("/api/audit/orders/1/timeline", "Cliente").andExpect(status().isForbidden());
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get("/api/report/kpis")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/audit/events")).andExpect(status().isUnauthorized());
    }
}
