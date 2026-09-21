package cl.duoc.pedidos360.bff.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.bff.client.CatalogClient;
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

    public ProductoDto buscarPorId(Long id) {
        return catalogClient.buscarPorId(id);
    }

    public ProductoDto crear(CrearProductoRequest request) {
        return catalogClient.crear(request);
    }
}
