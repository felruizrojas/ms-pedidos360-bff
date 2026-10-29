package cl.duoc.pedidos360.bff.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.dto.ActualizarPedidoRequest;
import cl.duoc.pedidos360.bff.dto.CambiarEstadoRequest;
import cl.duoc.pedidos360.bff.dto.CrearPedidoRequest;
import cl.duoc.pedidos360.bff.dto.HistorialEstadoDto;
import cl.duoc.pedidos360.bff.dto.PaginaDto;
import cl.duoc.pedidos360.bff.dto.PedidoDto;
import cl.duoc.pedidos360.bff.service.OrdersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Reenvía a ms-pedidos360-orders, que aplica las reglas de negocio y la autorización fina por rol y dueño.
 * Los errores van a nivel de clase; cada método declara su respuesta exitosa (si no, springdoc omite el 200).
 */
@Tag(name = "Pedidos", description = "Reenvía a ms-pedidos360-orders")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Validación (BFF u orders), estado desconocido o producto inexistente"),
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
    @ApiResponse(responseCode = "403", description = "Falta el scope o el rol, o orders rechazó la operación para ese rol"),
    @ApiResponse(responseCode = "404", description = "El pedido no existe (o no es del cliente)"),
    @ApiResponse(responseCode = "409", description = "Transición inválida, stock insuficiente o conflicto de versión"),
    @ApiResponse(responseCode = "502", description = "orders respondió un error inesperado"),
    @ApiResponse(responseCode = "503", description = "orders (o catalog, detrás de orders) no disponible")
})
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrdersService ordersService;

    @Operation(summary = "Crear pedido (queda en CREADO; precios desde catalog)")
    @ApiResponse(responseCode = "201", description = "Pedido creado")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto crear(@Valid @RequestBody CrearPedidoRequest request) {
        return ordersService.crear(request);
    }

    @Operation(summary = "Obtener pedido (un Cliente solo ve los suyos)")
    @ApiResponse(responseCode = "200", description = "Pedido")
    @GetMapping("/{id}")
    public PedidoDto obtener(@PathVariable Long id) {
        return ordersService.obtener(id);
    }

    @Operation(summary = "Listar pedidos paginados, más recientes primero (un Cliente solo recibe los suyos)")
    @ApiResponse(responseCode = "200", description = "Página de pedidos")
    @GetMapping
    public PaginaDto<PedidoDto> listar(
            @Parameter(description = "Filtra por estado", example = "CREADO") @RequestParam(required = false) String estado,
            @Parameter(description = "creadoEn >= desde (ISO-8601 o fecha sola)", example = "2026-10-01")
            @RequestParam(required = false) String desde,
            @Parameter(description = "creadoEn <= hasta (una fecha sola incluye el día completo)", example = "2026-10-31")
            @RequestParam(required = false) String hasta,
            @Parameter(description = "Página (desde 0)") @RequestParam(required = false) Integer page,
            @Parameter(description = "Tamaño de página (1 a 100)") @RequestParam(required = false) Integer size) {
        return ordersService.listar(estado, desde, hasta, page, size);
    }

    @Operation(summary = "Editar ítems y/o dirección (solo en CREADO)")
    @ApiResponse(responseCode = "200", description = "Pedido actualizado")
    @PutMapping("/{id}")
    public PedidoDto actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarPedidoRequest request) {
        return ordersService.actualizar(id, request);
    }

    @Operation(summary = "Cambiar estado",
            description = "Operador: cualquier transición válida. Cliente: solo CANCELADO de su pedido en CREADO.")
    @ApiResponse(responseCode = "200", description = "Pedido con el estado nuevo")
    @PutMapping("/{id}/status")
    public PedidoDto cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return ordersService.cambiarEstado(id, request);
    }

    @Operation(summary = "Historial de estados (más antiguo primero)")
    @ApiResponse(responseCode = "200", description = "Historial")
    @GetMapping("/{id}/history")
    public List<HistorialEstadoDto> historial(@PathVariable Long id) {
        return ordersService.historial(id);
    }

    @Operation(summary = "Eliminar pedido (borrado lógico: queda visible como Eliminado; solo Operador; avisa al cliente)")
    @ApiResponse(responseCode = "204", description = "Pedido eliminado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        ordersService.eliminar(id);
    }
}
