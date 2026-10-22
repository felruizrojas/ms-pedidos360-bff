package cl.duoc.pedidos360.bff.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.bff.client.CatalogClient;
import cl.duoc.pedidos360.bff.dto.ActualizarProductoRequest;
import cl.duoc.pedidos360.bff.dto.CrearProductoRequest;
import cl.duoc.pedidos360.bff.dto.ProductoDto;
import lombok.RequiredArgsConstructor;

/** Punto de composición/agregación del dominio catálogo (hoy delega 1:1 en el cliente). */
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CatalogClient catalogClient;

    public List<ProductoDto> listar() {
        return catalogClient.listar();
    }

    public List<ProductoDto> listarEliminados() {
        return catalogClient.listarEliminados();
    }

    public ProductoDto buscarPorId(Long id) {
        return catalogClient.buscarPorId(id);
    }

    public ProductoDto crear(CrearProductoRequest request) {
        return catalogClient.crear(request);
    }

    public ProductoDto actualizar(Long id, ActualizarProductoRequest request) {
        return catalogClient.actualizar(id, request);
    }

    public void eliminar(Long id) {
        catalogClient.eliminar(id);
    }

    public ProductoDto restaurar(Long id) {
        return catalogClient.restaurar(id);
    }
}
