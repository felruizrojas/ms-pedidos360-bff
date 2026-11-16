package cl.duoc.pedidos360.bff.service;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import cl.duoc.pedidos360.bff.client.AuditClient;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/** Punto de composición del dominio auditoría (hoy delega 1:1 en el cliente). */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditClient auditClient;

    public JsonNode eventos(MultiValueMap<String, String> params) {
        return auditClient.eventos(params);
    }

    public JsonNode evento(String eventId) {
        return auditClient.evento(eventId);
    }

    public JsonNode timelinePedido(String pedidoId) {
        return auditClient.timelinePedido(pedidoId);
    }

    public JsonNode timelineActor(String actorId, MultiValueMap<String, String> params) {
        return auditClient.timelineActor(actorId, params);
    }

    public JsonNode tiposDeEvento() {
        return auditClient.tiposDeEvento();
    }

    public JsonNode resumen(MultiValueMap<String, String> params) {
        return auditClient.resumen(params);
    }

    public JsonNode mensajesFallidos(MultiValueMap<String, String> params) {
        return auditClient.mensajesFallidos(params);
    }

    public JsonNode mensajeFallido(Long id) {
        return auditClient.mensajeFallido(id);
    }
}
