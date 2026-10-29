package cl.duoc.pedidos360.bff.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.bff.client.OrdersClient;
import cl.duoc.pedidos360.bff.dto.ActualizarPedidoRequest;
import cl.duoc.pedidos360.bff.dto.CambiarEstadoRequest;
import cl.duoc.pedidos360.bff.dto.CrearPedidoRequest;
import cl.duoc.pedidos360.bff.dto.HistorialEstadoDto;
import cl.duoc.pedidos360.bff.dto.PaginaDto;
import cl.duoc.pedidos360.bff.dto.PedidoDto;
import lombok.RequiredArgsConstructor;

/**
 * Punto de composición/agregación del dominio pedidos (hoy delega 1:1 en el cliente). La autorización fina
 * (dueño del pedido, quién puede cambiar a qué estado) la hace orders: no se duplica aquí.
 */
@Service
@RequiredArgsConstructor
public class OrdersService {

    private final OrdersClient ordersClient;

    public PedidoDto crear(CrearPedidoRequest request) {
        return ordersClient.crear(request);
    }

    public PedidoDto obtener(Long id) {
        return ordersClient.obtener(id);
    }

    public PaginaDto<PedidoDto> listar(String estado, String desde, String hasta, Integer page, Integer size) {
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("estado", estado);
        filtros.put("desde", desde);
        filtros.put("hasta", hasta);
        filtros.put("page", page);
        filtros.put("size", size);
        return ordersClient.listar(filtros);
    }

    public PedidoDto actualizar(Long id, ActualizarPedidoRequest request) {
        return ordersClient.actualizar(id, request);
    }

    public PedidoDto cambiarEstado(Long id, CambiarEstadoRequest request) {
        return ordersClient.cambiarEstado(id, request);
    }

    public List<HistorialEstadoDto> historial(Long id) {
        return ordersClient.historial(id);
    }

    public void eliminar(Long id) {
        ordersClient.eliminar(id);
    }
}
