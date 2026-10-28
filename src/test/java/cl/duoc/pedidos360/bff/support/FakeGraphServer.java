package cl.duoc.pedidos360.bff.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

/**
 * Entra ID + Microsoft Graph simulados: responde según el path (sin query) y registra los paths pedidos.
 * Un path sin respuesta configurada responde 404.
 */
public final class FakeGraphServer {

    private record Respuesta(int status, String body) { }

    private final HttpServer server;
    private final Map<String, Respuesta> rutas = new ConcurrentHashMap<>();
    private final List<String> recibidos = new CopyOnWriteArrayList<>();

    public FakeGraphServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            recibidos.add(exchange.getRequestMethod() + " " + path);
            Respuesta r = rutas.getOrDefault(path, new Respuesta(404, "{\"error\":{\"code\":\"Request_ResourceNotFound\"}}"));
            byte[] bytes = r.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(r.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });
        server.start();
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        rutas.clear();
        recibidos.clear();
    }

    public void respond(String path, int status, String body) {
        rutas.put(path, new Respuesta(status, body));
    }

    public List<String> recibidos() {
        return List.copyOf(recibidos);
    }
}
