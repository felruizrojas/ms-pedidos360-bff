package cl.duoc.pedidos360.bff.client;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import cl.duoc.pedidos360.bff.config.DownstreamProperties;
import cl.duoc.pedidos360.bff.config.GraphProperties;
import cl.duoc.pedidos360.bff.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/**
 * Cliente de Microsoft Graph con token propio del BFF (client credentials), nunca el del usuario.
 * Lista los usuarios que tienen asignado algún App role de la API: son los usuarios de la aplicación.
 * <ul>
 * <li>Graph caído o timeout → 503 · Graph responde 401/403 (secret inválido o sin consentimiento) u otro error → 502.</li>
 * <li>Sin GRAPH_CLIENT_SECRET → 503 con un mensaje que lo indica.</li>
 * </ul>
 */
@Slf4j
@Component
public class GraphClient {

    private static final String SERVICIO = "Microsoft Graph";
    /** appRoleId de "acceso por defecto" (asignación sin rol). */
    private static final String SIN_ROL = "00000000-0000-0000-0000-000000000000";
    private static final String CAMPOS_USUARIO = "id,displayName,givenName,surname,mail,userPrincipalName,"
            + "accountEnabled,userType,jobTitle,department,mobilePhone,createdDateTime";

    private final GraphProperties props;
    private final RestClient graph;
    private final RestClient login;

    private String token;
    private Instant tokenExpira = Instant.EPOCH;

    public GraphClient(GraphProperties props, DownstreamProperties timeouts) {
        this.props = props;
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeouts.connectTimeout())
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeouts.readTimeout());
        this.graph = construir(props.baseUrl(), factory);
        this.login = construir(props.authorityHost(), factory);
    }

    private static RestClient construir(String baseUrl, JdkClientHttpRequestFactory factory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    try {
                        return execution.execute(request, body);
                    } catch (IOException e) {
                        throw new ServicioNoDisponibleException(HttpStatus.SERVICE_UNAVAILABLE, SERVICIO, e);
                    }
                })
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    int code = response.getStatusCode().value();
                    if (code == 401 || code == 403) {
                        log.error("Microsoft Graph respondió {} en {}: revisa GRAPH_CLIENT_SECRET y el consentimiento "
                                + "de User.Read.All y Application.Read.All", code, request.getURI().getPath());
                    } else {
                        log.error("Microsoft Graph respondió {} en {}", code, request.getURI().getPath());
                    }
                    throw new ServicioNoDisponibleException(HttpStatus.BAD_GATEWAY, SERVICIO, null);
                })
                .build();
    }

    /**
     * Usuarios con algún App role de la API. Los asignados solo con "acceso por defecto" (sin rol) no cuentan
     * como usuarios de la aplicación, y los grupos asignados no se expanden.
     */
    public List<UsuarioGraph> usuariosConRol() {
        if (!props.configurado()) {
            throw ServicioNoDisponibleException.conMensaje(HttpStatus.SERVICE_UNAVAILABLE,
                    "Microsoft Graph no está configurado en el BFF (falta GRAPH_CLIENT_SECRET)");
        }
        String bearer = obtenerToken();

        JsonNode sp = graph.get().uri("/servicePrincipals(appId='{appId}')?$select=id,appRoles", props.apiAppId())
                .headers(h -> h.setBearerAuth(bearer))
                .retrieve().body(JsonNode.class);
        Map<String, String> nombresRol = new HashMap<>();
        for (JsonNode rol : sp.path("appRoles")) {
            nombresRol.put(rol.path("id").asString(), rol.path("value").asString());
        }

        JsonNode asignaciones = graph.get()
                .uri("/servicePrincipals/{id}/appRoleAssignedTo?$top=999", sp.path("id").asString())
                .headers(h -> h.setBearerAuth(bearer))
                .retrieve().body(JsonNode.class);
        Map<String, List<String>> rolesPorUsuario = new LinkedHashMap<>();
        for (JsonNode a : asignaciones.path("value")) {
            if (!"User".equals(a.path("principalType").asString())) {
                continue;
            }
            List<String> roles = rolesPorUsuario.computeIfAbsent(a.path("principalId").asString(), k -> new ArrayList<>());
            String rol = nombresRol.get(a.path("appRoleId").asString());
            if (rol != null && !SIN_ROL.equals(a.path("appRoleId").asString()) && !roles.contains(rol)) {
                roles.add(rol);
            }
        }

        rolesPorUsuario.values().removeIf(List::isEmpty);

        List<UsuarioGraph> usuarios = new ArrayList<>();
        rolesPorUsuario.forEach((id, roles) -> {
            JsonNode u = graph.get().uri("/users/{id}?$select=" + CAMPOS_USUARIO, id)
                    .headers(h -> h.setBearerAuth(bearer))
                    .retrieve()
                    .onStatus(s -> s.value() == 404, (req, res) -> { })
                    .body(JsonNode.class);
            if (u != null && u.hasNonNull("id")) {
                usuarios.add(mapear(u, roles));
            }
        });
        return usuarios;
    }

    private synchronized String obtenerToken() {
        if (token != null && Instant.now().isBefore(tokenExpira)) {
            return token;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("scope", "https://graph.microsoft.com/.default");
        JsonNode respuesta = login.post().uri("/{tenant}/oauth2/v2.0/token", props.tenantId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(JsonNode.class);
        token = respuesta.path("access_token").asString();
        // Se renueva un minuto antes de que venza.
        tokenExpira = Instant.now().plusSeconds(Math.max(respuesta.path("expires_in").asLong(3600) - 60, 0));
        return token;
    }

    private static UsuarioGraph mapear(JsonNode u, List<String> roles) {
        String creado = texto(u, "createdDateTime");
        return new UsuarioGraph(
                texto(u, "id"),
                texto(u, "displayName"),
                texto(u, "givenName"),
                texto(u, "surname"),
                texto(u, "mail"),
                texto(u, "userPrincipalName"),
                u.hasNonNull("accountEnabled") ? u.path("accountEnabled").asBoolean() : null,
                texto(u, "userType"),
                texto(u, "jobTitle"),
                texto(u, "department"),
                texto(u, "mobilePhone"),
                creado != null ? Instant.parse(creado) : null,
                List.copyOf(roles));
    }

    private static String texto(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) ? nodo.path(campo).asString() : null;
    }
}
