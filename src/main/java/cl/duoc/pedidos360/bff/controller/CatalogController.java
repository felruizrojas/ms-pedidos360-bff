package cl.duoc.pedidos360.bff.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.dto.CrearProductoRequest;
import cl.duoc.pedidos360.bff.dto.ProductoDto;
import cl.duoc.pedidos360.bff.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/catalog/products")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public List<ProductoDto> listar() {
        return catalogService.listar();
    }

    @GetMapping("/{id}")
    public ProductoDto buscarPorId(@PathVariable Long id) {
        return catalogService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDto crear(@Valid @RequestBody CrearProductoRequest request) {
        return catalogService.crear(request);
    }
}
