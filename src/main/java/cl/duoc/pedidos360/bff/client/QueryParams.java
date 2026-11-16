package cl.duoc.pedidos360.bff.client;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilder;

/**
 * Reenvía los query params tal cual llegaron (incluidos los repetidos, p. ej. {@code ?accion=A&accion=B}).
 * Cada valor va como variable de plantilla, así se codifica completo (el "+" de "-03:00" o "+02:00"), igual que
 * en {@link OrdersClient}.
 */
final class QueryParams {

    private QueryParams() {
    }

    static URI conQuery(UriBuilder builder, MultiValueMap<String, String> params, Object... pathVariables) {
        Map<String, Object> variables = new LinkedHashMap<>();
        for (int i = 0; i < pathVariables.length; i++) {
            variables.put("v" + i, pathVariables[i]);
        }
        int n = 0;
        if (params != null) {
            for (Map.Entry<String, List<String>> e : params.entrySet()) {
                for (String valor : e.getValue()) {
                    String nombre = "q" + n++;
                    builder.queryParam(e.getKey(), "{" + nombre + "}");
                    variables.put(nombre, valor);
                }
            }
        }
        return builder.build(variables);
    }
}
