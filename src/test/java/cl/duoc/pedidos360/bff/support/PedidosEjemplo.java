package cl.duoc.pedidos360.bff.support;

/** JSON de ejemplo copiados del contrato de ms-pedidos360-orders (README, "Contrato para otros servicios"). */
public final class PedidosEjemplo {

    public static final String PEDIDO = """
            {
              "id": 15,
              "clienteId": "4f6c2a1e-1111-2222-3333-444455556666",
              "clienteNombre": "María Pérez",
              "clienteEmail": "maria@contoso.cl",
              "direccionEntrega": "Av. Providencia 1234, Santiago",
              "estado": "ACEPTADO",
              "total": 2700.00,
              "stockDescontado": true,
              "items": [
                { "id": 31, "productoId": 1, "productoNombre": "Pan Amasado", "precioUnitario": 600.00, "cantidad": 2, "subtotal": 1200.00 },
                { "id": 32, "productoId": 4, "productoNombre": "Café Americano", "precioUnitario": 1500.00, "cantidad": 1, "subtotal": 1500.00 }
              ],
              "creadoEn": "2026-10-20T15:04:05.123Z",
              "aceptadoEn": "2026-10-20T15:10:00.456Z",
              "enPreparacionEn": null,
              "despachadoEn": null,
              "entregadoEn": null,
              "canceladoEn": null,
              "motivoCancelacion": null,
              "version": 1
            }
            """;

    public static final String CREAR = """
            { "direccionEntrega": "Av. Providencia 1234, Santiago",
              "items": [ { "productoId": 1, "cantidad": 2 }, { "productoId": 4, "cantidad": 1 } ] }
            """;

    public static final String PAGINA = "{\"contenido\":[" + PEDIDO + "],"
            + "\"pagina\":0,\"tamano\":20,\"totalElementos\":1,\"totalPaginas\":1}";

    public static final String HISTORIAL = """
            [
              { "estadoAnterior": null, "estadoNuevo": "CREADO", "usuarioId": "4f6c", "usuarioNombre": "María Pérez",
                "usuarioRol": "Cliente", "fecha": "2026-10-20T15:04:05.123Z", "comentario": null },
              { "estadoAnterior": "CREADO", "estadoNuevo": "ACEPTADO", "usuarioId": "9a1b", "usuarioNombre": "Olga Operadora",
                "usuarioRol": "Operador", "fecha": "2026-10-20T15:10:00.456Z", "comentario": null }
            ]
            """;

    private PedidosEjemplo() {
    }
}
