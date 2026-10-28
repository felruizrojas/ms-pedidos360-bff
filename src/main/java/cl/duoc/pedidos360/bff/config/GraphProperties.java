package cl.duoc.pedidos360.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Acceso a Microsoft Graph con client credentials (pedidos360.graph.*). Usa el app registration de la API:
 * necesita un client secret y los permisos de aplicación User.Read.All y Application.Read.All con
 * consentimiento de administrador. Sin secret, /api/users responde 503.
 *
 * @param apiAppId client ID de la API cuyos App roles (Admin/Operador/Cliente) definen quién aparece.
 */
@ConfigurationProperties("pedidos360.graph")
public record GraphProperties(String tenantId, String clientId, String clientSecret, String apiAppId,
        String authorityHost, String baseUrl) {

    public GraphProperties {
        authorityHost = authorityHost != null && !authorityHost.isBlank() ? authorityHost : "https://login.microsoftonline.com";
        baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl : "https://graph.microsoft.com/v1.0";
        apiAppId = apiAppId != null && !apiAppId.isBlank() ? apiAppId : clientId;
    }

    public boolean configurado() {
        return noVacio(tenantId) && noVacio(clientId) && noVacio(clientSecret);
    }

    private static boolean noVacio(String valor) {
        return valor != null && !valor.isBlank();
    }
}
