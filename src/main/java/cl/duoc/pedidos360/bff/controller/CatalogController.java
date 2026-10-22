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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.dto.ActualizarProductoRequest;
import cl.duoc.pedidos360.bff.dto.CrearProductoRequest;
import cl.duoc.pedidos360.bff.dto.ProductoDto;
import cl.duoc.pedidos360.bff.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    @Operation(summary = "Listar productos eliminados (borrado lógico)",
            description = "Para que Admin/Operador puedan deshacer la eliminación.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Productos eliminados"),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Falta el scope o el rol Admin/Operador (ENFORCE_ROLES=true)"),
        @ApiResponse(responseCode = "503", description = "Servicio de catálogo no disponible")
    })
    @GetMapping("/eliminados")
    public List<ProductoDto> listarEliminados() {
        return catalogService.listarEliminados();
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

    @PutMapping("/{id}")
    public ProductoDto actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarProductoRequest request) {
        return catalogService.actualizar(id, request);
    }

    @Operation(summary = "Eliminar producto (borrado lógico)",
            description = "Deja de listarse y de poder pedirse; los pedidos ya creados lo conservan.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Producto eliminado"),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Falta el scope o el rol Admin/Operador (ENFORCE_ROLES=true)"),
        @ApiResponse(responseCode = "404", description = "El producto no existe o ya fue eliminado"),
        @ApiResponse(responseCode = "503", description = "Servicio de catálogo no disponible")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        catalogService.eliminar(id);
    }

    @Operation(summary = "Restaurar producto eliminado",
            description = "Deshace el borrado lógico: vuelve a listarse y a poder pedirse. Idempotente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto restaurado"),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Falta el scope o el rol Admin/Operador (ENFORCE_ROLES=true)"),
        @ApiResponse(responseCode = "404", description = "El producto no existe"),
        @ApiResponse(responseCode = "503", description = "Servicio de catálogo no disponible")
    })
    @PutMapping("/{id}/restaurar")
    public ProductoDto restaurar(@PathVariable Long id) {
        return catalogService.restaurar(id);
    }
}
