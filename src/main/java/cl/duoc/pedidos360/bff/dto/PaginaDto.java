package cl.duoc.pedidos360.bff.dto;

import java.util.List;

/** Listado paginado de orders (nombres en español, igual que su contrato). */
public record PaginaDto<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas) {
}
