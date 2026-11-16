package cl.duoc.pedidos360.bff.controller;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * Reenvía a ms-pedidos360-audit (solo lectura; rol Admin o Auditor, que audit vuelve a exigir). Los query params
 * pasan tal cual (incluidos los repetidos, como accion=A&amp;accion=B) y la respuesta es el JSON de audit.
 */
@Tag(name = "Auditoría", description = "Reenvía a ms-pedidos360-audit (timeline quién/qué/cuándo/desde dónde y DLT)")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Filtro inválido o desde > hasta"),
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
    @ApiResponse(responseCode = "403", description = "Falta el scope o el rol Admin/Auditor"),
    @ApiResponse(responseCode = "404", description = "El evento, el timeline o el mensaje fallido no existe"),
    @ApiResponse(responseCode = "502", description = "audit respondió un error inesperado"),
    @ApiResponse(responseCode = "503", description = "audit no disponible")
})
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    static final String FILTROS = "Query params de audit, tal cual: usuario, accion (repetible), entityType, entityId, "
            + "rol, desde, hasta, correlationId, page, size, sort";

    private final AuditService auditService;

    @Operation(summary = "Listado paginado de eventos (filtros combinables)")
    @ApiResponse(responseCode = "200", description = "Página de eventos")
    @GetMapping("/events")
    public JsonNode eventos(@Parameter(description = FILTROS) @RequestParam MultiValueMap<String, String> params) {
        return auditService.eventos(params);
    }

    @Operation(summary = "Detalle de un evento, con el mensaje original")
    @ApiResponse(responseCode = "200", description = "Evento")
    @GetMapping("/events/{eventId}")
    public JsonNode evento(@PathVariable String eventId) {
        return auditService.evento(eventId);
    }

    @Operation(summary = "Timeline completo de un pedido, más antiguo primero")
    @ApiResponse(responseCode = "200", description = "Timeline")
    @GetMapping("/orders/{pedidoId}/timeline")
    public JsonNode timelinePedido(@PathVariable String pedidoId) {
        return auditService.timelinePedido(pedidoId);
    }

    @Operation(summary = "Acciones de un usuario, más reciente primero")
    @ApiResponse(responseCode = "200", description = "Página de eventos del usuario")
    @GetMapping("/actors/{actorId}/timeline")
    public JsonNode timelineActor(@PathVariable String actorId,
            @Parameter(description = "desde, hasta, page, size") @RequestParam MultiValueMap<String, String> params) {
        return auditService.timelineActor(actorId, params);
    }

    @Operation(summary = "Acciones presentes con su conteo (filtro del front)")
    @ApiResponse(responseCode = "200", description = "Tipos de evento")
    @GetMapping("/event-types")
    public JsonNode tiposDeEvento() {
        return auditService.tiposDeEvento();
    }

    @Operation(summary = "Conteo por acción, por rol y top 5 actores (default: últimas 24 h)")
    @ApiResponse(responseCode = "200", description = "Resumen")
    @GetMapping("/summary")
    public JsonNode resumen(@Parameter(description = "desde, hasta") @RequestParam MultiValueMap<String, String> params) {
        return auditService.resumen(params);
    }

    @Operation(summary = "Mensajes que terminaron en un DLT (sin stacktrace)")
    @ApiResponse(responseCode = "200", description = "Página de mensajes fallidos")
    @GetMapping("/dead-letters")
    public JsonNode mensajesFallidos(@Parameter(description = "topic, desde, hasta, page, size")
            @RequestParam MultiValueMap<String, String> params) {
        return auditService.mensajesFallidos(params);
    }

    @Operation(summary = "Detalle de un mensaje fallido (stacktrace y mensaje original)")
    @ApiResponse(responseCode = "200", description = "Mensaje fallido")
    @GetMapping("/dead-letters/{id}")
    public JsonNode mensajeFallido(@PathVariable Long id) {
        return auditService.mensajeFallido(id);
    }
}
