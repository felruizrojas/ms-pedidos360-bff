package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.FakeGraphServer;
import cl.duoc.pedidos360.bff.support.TestJwt;

/** GET /api/users: usuarios de Entra ID (Graph simulado) con su estado de inicio de sesión. */
class UsersEndpointsTest extends AbstractBffTest {

    private static final String URL = "/api/users";
    private static final FakeGraphServer GRAPH = new FakeGraphServer();
    private static final String SP = "/servicePrincipals(appId='" + TestJwt.AUDIENCE + "')";

    @DynamicPropertySource
    static void graph(DynamicPropertyRegistry registry) {
        registry.add("pedidos360.graph.authority-host", GRAPH::baseUrl);
        registry.add("pedidos360.graph.base-url", GRAPH::baseUrl);
        registry.add("pedidos360.graph.tenant-id", () -> "test-tenant");
        registry.add("pedidos360.graph.client-id", () -> TestJwt.AUDIENCE);
        registry.add("pedidos360.graph.api-app-id", () -> TestJwt.AUDIENCE);
        registry.add("pedidos360.graph.client-secret", () -> "secreto-test");
    }

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void graphConTresUsuarios() {
        GRAPH.reset();
        GRAPH.respond("/test-tenant/oauth2/v2.0/token", 200, "{\"access_token\":\"tok-graph\",\"expires_in\":3600}");
        GRAPH.respond(SP, 200, """
                { "id": "sp-1", "appRoles": [
                    { "id": "r-admin", "value": "Admin" },
                    { "id": "r-oper", "value": "Operador" },
                    { "id": "r-cli", "value": "Cliente" } ] }
                """);
        GRAPH.respond("/servicePrincipals/sp-1/appRoleAssignedTo", 200, """
                { "value": [
                    { "principalId": "u-cli", "principalType": "User", "appRoleId": "r-cli" },
                    { "principalId": "u-admin", "principalType": "User", "appRoleId": "r-admin" },
                    { "principalId": "u-oper", "principalType": "User", "appRoleId": "r-oper" },
                    { "principalId": "u-sin-rol", "principalType": "User", "appRoleId": "00000000-0000-0000-0000-000000000000" },
                    { "principalId": "g-1", "principalType": "Group", "appRoleId": "r-cli" } ] }
                """);
        GRAPH.respond("/users/u-admin", 200, usuario("u-admin", "Ana Admin"));
        GRAPH.respond("/users/u-oper", 200, usuario("u-oper", "Óscar Operador"));
        GRAPH.respond("/users/u-cli", 200, usuario("u-cli", "Carla Cliente"));
    }

    private static String usuario(String id, String nombre) {
        return """
                { "id": "%s", "displayName": "%s", "givenName": "%s", "surname": "Prueba",
                  "mail": "%s@contoso.cl", "userPrincipalName": "%s@contoso.onmicrosoft.com",
                  "accountEnabled": true, "userType": "Member", "jobTitle": null, "department": null,
                  "mobilePhone": null, "createdDateTime": "2026-09-01T12:00:00Z" }
                """.formatted(id, nombre, nombre.split(" ")[0], id, id);
    }

    private ResultActions as(TestJwt jwt, String url) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, jwt.bearer()));
    }

    @Test
    void adminVeLosUsuariosOrdenadosConRolesYEstado() throws Exception {
        // El Operador inició sesión (cualquier request autenticado lo registra); el Cliente no.
        as(TestJwt.valid().roles("Operador").oid("u-oper").name("Óscar Operador"), URL + "/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("u-oper"))
                .andExpect(jsonPath("$.primerAcceso").isNotEmpty());

        as(TestJwt.valid().roles("Admin").oid("u-admin"), URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nombre").value("Ana Admin"))
                .andExpect(jsonPath("$[0].roles[0]").value("Admin"))
                .andExpect(jsonPath("$[0].activo").value(true))
                .andExpect(jsonPath("$[1].nombre").value("Carla Cliente"))
                .andExpect(jsonPath("$[1].activo").value(false))
                .andExpect(jsonPath("$[1].ultimoAcceso").doesNotExist())
                .andExpect(jsonPath("$[1].email").value("u-cli@contoso.cl"))
                .andExpect(jsonPath("$[1].cuentaHabilitada").value(true))
                .andExpect(jsonPath("$[2].nombre").value("Óscar Operador"))
                .andExpect(jsonPath("$[2].roles[0]").value("Operador"))
                .andExpect(jsonPath("$[2].activo").value(true))
                .andExpect(jsonPath("$[2].ultimoAcceso").isNotEmpty());

        // Ni los grupos ni los usuarios con "acceso por defecto" (sin rol) se consultan como usuarios.
        assertThat(GRAPH.recibidos()).noneMatch(r -> r.contains("/users/g-1") || r.contains("/users/u-sin-rol"));
    }

    @Test
    void operadorYClienteNoVenUsuarios() throws Exception {
        as(TestJwt.valid().roles("Operador"), URL).andExpect(status().isForbidden());
        as(TestJwt.valid().roles("Cliente"), URL).andExpect(status().isForbidden());
        assertThat(GRAPH.recibidos()).isEmpty();
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void graphRechazaElSecretDa502() throws Exception {
        GRAPH.respond("/test-tenant/oauth2/v2.0/token", 401, "{\"error\":\"invalid_client\"}");
        // El token de Graph puede estar en caché de otro test: se fuerza el error también en Graph.
        GRAPH.respond(SP, 403, "{\"error\":{\"code\":\"Authorization_RequestDenied\"}}");
        as(TestJwt.valid().roles("Admin"), URL)
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensaje").value("Servicio de Microsoft Graph no disponible"));
    }

    @Test
    void usuarioBorradoEnEntraNoAparece() throws Exception {
        GRAPH.respond("/users/u-cli", 404, "{\"error\":{\"code\":\"Request_ResourceNotFound\"}}");
        as(TestJwt.valid().roles("Admin"), URL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
