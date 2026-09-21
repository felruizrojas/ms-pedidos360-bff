package cl.duoc.pedidos360.bff.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.pedidos360.bff.dto.CrearProductoRequest;
import cl.duoc.pedidos360.bff.dto.ProductoDto;
import cl.duoc.pedidos360.bff.exception.ProductoNoEncontradoException;

/** Cliente de ms-pedidos360-catalog. */
@Component
public class CatalogClient {

    private static final String PRODUCTS = "/api/catalog/products";

    private final RestClient restClient;

    public CatalogClient(DownstreamRestClientFactory factory,
            @Value("${pedidos360.services.catalog.base-url}") String baseUrl) {
        this.restClient = factory.create("catálogo", baseUrl);
    }

    public List<ProductoDto> listar() {
        return restClient.get().uri(PRODUCTS)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ProductoDto>>() { });
    }

    public ProductoDto buscarPorId(Long id) {
        return restClient.get().uri(PRODUCTS + "/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                    throw new ProductoNoEncontradoException(id);
                })
                .body(ProductoDto.class);
    }

    public ProductoDto crear(CrearProductoRequest request) {
        return restClient.post().uri(PRODUCTS)
                .body(request)
                .retrieve()
                .body(ProductoDto.class);
    }
}
