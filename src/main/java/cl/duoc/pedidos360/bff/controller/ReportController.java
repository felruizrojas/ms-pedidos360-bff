package cl.duoc.pedidos360.bff.controller;

import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * Reenvía a ms-pedidos360-report (solo lectura; rol Admin, que report vuelve a exigir). Los query params
 * (range, desde, hasta, limit, orden) pasan tal cual y la respuesta es el JSON de report. Ver su README.
 */
@Tag(name = "Reportería", description = "Reenvía a ms-pedidos360-report (KPIs calculados desde Kafka)")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "range/desde/hasta/limit/orden inválidos o rango > 90 días"),
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
    @ApiResponse(responseCode = "403", description = "Falta el scope o el rol Admin"),
    @ApiResponse(responseCode = "502", description = "report respondió un error inesperado"),
    @ApiResponse(responseCode = "503", description = "report no disponible")
})
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    static final String PARAMS = "Query params de report, tal cual: range (last24h, last7d, last30d, today), desde, hasta";

    private final ReportService reportService;

    @Operation(summary = "Panel de KPIs: ventas, pedidos, cancelación, clientes activos, lead time y estados activos")
    @ApiResponse(responseCode = "200", description = "KPIs del rango")
    @GetMapping("/kpis")
    public JsonNode kpis(@Parameter(description = PARAMS) @RequestParam MultiValueMap<String, String> params) {
        return reportService.kpis(params);
    }

    @Operation(summary = "Ventas por hora (por día si el rango supera 48 h), para SalesChart")
    @ApiResponse(responseCode = "200", description = "Serie de ventas")
    @GetMapping("/sales-by-hour")
    public JsonNode ventasPorHora(@Parameter(description = PARAMS) @RequestParam MultiValueMap<String, String> params) {
        return reportService.ventasPorHora(params);
    }

    @Operation(summary = "Lead time: resumen, distribución y serie diaria, para LeadTimeChart")
    @ApiResponse(responseCode = "200", description = "Lead time del rango")
    @GetMapping("/lead-time")
    public JsonNode leadTime(@Parameter(description = PARAMS) @RequestParam MultiValueMap<String, String> params) {
        return reportService.leadTime(params);
    }

    @Operation(summary = "Productos más vendidos, para TopProductsChart (limit 1-50, orden cantidad|monto)")
    @ApiResponse(responseCode = "200", description = "Top productos")
    @GetMapping("/top-products")
    public JsonNode topProductos(@Parameter(description = PARAMS + ", limit, orden")
            @RequestParam MultiValueMap<String, String> params) {
        return reportService.topProductos(params);
    }

    @Operation(summary = "Pedidos activos ahora por estado y los 10 más antiguos")
    @ApiResponse(responseCode = "200", description = "Estados activos")
    @GetMapping("/active-states")
    public JsonNode estadosActivos() {
        return reportService.estadosActivos();
    }
}
