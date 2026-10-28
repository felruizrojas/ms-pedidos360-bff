package cl.duoc.pedidos360.bff.client;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import cl.duoc.pedidos360.bff.dto.ActualizarPedidoRequest;
import cl.duoc.pedidos360.bff.dto.CambiarEstadoRequest;
import cl.duoc.pedidos360.bff.dto.CrearPedidoRequest;
import cl.duoc.pedidos360.bff.dto.HistorialEstadoDto;
import cl.duoc.pedidos360.bff.dto.MensajeComandoDto;
import cl.duoc.pedidos360.bff.dto.NotificacionDto;
import cl.duoc.pedidos360.bff.dto.PaginaDto;
import cl.duoc.pedidos360.bff.dto.PedidoDto;

/**
 * Cliente de ms-pedidos360-orders. 404, 409 y 503 se propagan con el mensaje de orders
 * ({@link DownstreamRestClientFactory#PROPAGAR_NEGOCIO}); el resto, igual que catalog.
 */
@Component
public class OrdersClient {

    private static final String ORDERS = "/api/orders";

    private final RestClient restClient;

    public OrdersClient(DownstreamRestClientFactory factory,
            @Value("${pedidos360.services.orders.base-url}") String baseUrl) {
        this.restClient = factory.create("pedidos", baseUrl, DownstreamRestClientFactory.PROPAGAR_NEGOCIO);
    }

    public PedidoDto crear(CrearPedidoRequest request) {
        return restClient.post().uri(ORDERS)
                .body(request)
                .retrieve()
                .body(PedidoDto.class);
    }

    public PedidoDto obtener(Long id) {
        return restClient.get().uri(ORDERS + "/{id}", id)
                .retrieve()
                .body(PedidoDto.class);
    }

    /** Reenvía solo los filtros que vinieron (null = no enviar), con su valor tal cual (orders los valida). */
    public PaginaDto<PedidoDto> listar(Map<String, Object> filtros) {
        Map<String, Object> presentes = new LinkedHashMap<>();
        filtros.forEach((k, v) -> {
            if (v != null) {
                presentes.put(k, v);
            }
        });
        return restClient.get()
                .uri(builder -> conQuery(builder.path(ORDERS), presentes))
                .retrieve()
                .body(new ParameterizedTypeReference<PaginaDto<PedidoDto>>() { });
    }

    public PedidoDto actualizar(Long id, ActualizarPedidoRequest request) {
        return restClient.put().uri(ORDERS + "/{id}", id)
                .body(request)
                .retrieve()
                .body(PedidoDto.class);
    }

    public PedidoDto cambiarEstado(Long id, CambiarEstadoRequest request) {
        return restClient.put().uri(ORDERS + "/{id}/status", id)
                .body(request)
                .retrieve()
                .body(PedidoDto.class);
    }

    public List<HistorialEstadoDto> historial(Long id) {
        return restClient.get().uri(ORDERS + "/{id}/history", id)
                .retrieve()
                .body(new ParameterizedTypeReference<List<HistorialEstadoDto>>() { });
    }

    /** Borrado lógico en orders (204). */
    public void eliminar(Long id) {
        restClient.delete().uri(ORDERS + "/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }

    public List<MensajeComandoDto> mensajes(Long id) {
        return restClient.get().uri(ORDERS + "/{id}/messages", id)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MensajeComandoDto>>() { });
    }

    public List<NotificacionDto> notificaciones() {
        return restClient.get().uri(ORDERS + "/notifications")
                .retrieve()
                .body(new ParameterizedTypeReference<List<NotificacionDto>>() { });
    }

    public void marcarNotificacionesLeidas() {
        restClient.put().uri(ORDERS + "/notifications/read")
                .retrieve()
                .toBodilessEntity();
    }

    /** Cada valor va como variable de plantilla: se codifica completo (p. ej. el "+" de "-03:00" o "+02:00"). */
    private static URI conQuery(UriBuilder builder, Map<String, Object> params) {
        params.keySet().forEach(k -> builder.queryParam(k, "{" + k + "}"));
        return builder.build(params);
    }
}
