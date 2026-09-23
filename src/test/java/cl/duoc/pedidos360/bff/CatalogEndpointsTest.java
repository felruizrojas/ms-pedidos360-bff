package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import cl.duoc.pedidos360.bff.support.AbstractBffTest;
import cl.duoc.pedidos360.bff.support.TestJwt;

class CatalogEndpointsTest extends AbstractBffTest {

    private static final String URL = "/api/catalog/products";
    private static final String PRODUCTO =
            "{\"id\":7,\"nombre\":\"Teclado\",\"descripcion\":\"Mecánico\",\"precio\":49990.50,\"stock\":12}";
    private static final String NUEVO = "{\"nombre\":\"Teclado\",\"descripcion\":\"Mecánico\",\"precio\":49990.50,\"stock\":12}";

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        CATALOG.respond(200, "[]");
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req) {
        return req.header(HttpHeaders.AUTHORIZATION, TestJwt.valid().bearer());
    }

    @Test
    void listaMapeaJsonDelCatalogo() throws Exception {
        CATALOG.respond(200, "[" + PRODUCTO + "]");
        mvc.perform(auth(get(URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nombre").value("Teclado"))
                .andExpect(jsonPath("$[0].descripcion").value("Mecánico"))
                .andExpect(jsonPath("$[0].precio").value(49990.50))
                .andExpect(jsonPath("$[0].stock").value(12));
        assertThat(CATALOG.last().path()).isEqualTo(URL);
    }

    @Test
    void buscarPorIdMapeaJson() throws Exception {
        CATALOG.respond(200, PRODUCTO);
        mvc.perform(auth(get(URL + "/7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.precio").value(49990.50));
        assertThat(CATALOG.last().path()).isEqualTo(URL + "/7");
    }

    @Test
    void headerAuthorizationYRequestIdLleganAlCatalogo() throws Exception {
        String bearer = TestJwt.valid().bearer();
        mvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer).header("X-Request-Id", "req-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "req-123"));
        assertThat(CATALOG.last().authorization()).isEqualTo(bearer);
        assertThat(CATALOG.last().requestId()).isEqualTo("req-123");
    }

    @Test
    void generaRequestIdSiNoVieneYLoReenvia() throws Exception {
        String id = mvc.perform(auth(get(URL))).andReturn().getResponse().getHeader("X-Request-Id");
        assertThat(id).isNotBlank();
        assertThat(CATALOG.last().requestId()).isEqualTo(id);
    }

    @Test
    void noEncontradoEnCatalogoDa404() throws Exception {
        CATALOG.respond(404, "{\"status\":404,\"mensaje\":\"interno\"}");
        mvc.perform(auth(get(URL + "/99")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Producto no encontrado con id 99"))
                .andExpect(jsonPath("$.path").value(URL + "/99"));
    }

    @Test
    void idInvalidoDa400() throws Exception {
        mvc.perform(auth(get(URL + "/abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Parámetro inválido: id"));
    }

    @Test
    void postValidoDa201() throws Exception {
        CATALOG.respond(201, PRODUCTO);
        mvc.perform(auth(post(URL)).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
        assertThat(CATALOG.last().method()).isEqualTo("POST");
        assertThat(CATALOG.last().body()).contains("\"nombre\":\"Teclado\"");
    }

    @Test
    void postInvalidoDa400ConDetallesSinLlamarAlCatalogo() throws Exception {
        mvc.perform(auth(post(URL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"precio\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").exists())
                .andExpect(jsonPath("$.detalles.precio").exists())
                .andExpect(jsonPath("$.detalles.stock").exists())
                .andExpect(jsonPath("$.path").value(URL));
        assertThat(CATALOG.last()).isNull();
    }

    @Test
    void nombreConNumerosRechazadoPorElBffAntesDeLlamarAlCatalogo() throws Exception {
        // Mismas reglas que catalog.ProductoRequest: el BFF debe fallar rápido, en español,
        // sin ni siquiera invocar al microservicio downstream.
        mvc.perform(auth(post(URL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Teclado123\",\"precio\":100,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre")
                        .value("El nombre solo admite letras y espacios (sin números ni símbolos)."));
        assertThat(CATALOG.last()).isNull();
    }

    @Test
    void precioSobreElTopeRechazadoPorElBffAntesDeLlamarAlCatalogo() throws Exception {
        mvc.perform(auth(post(URL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Teclado\",\"precio\":10000000000000000000000000000,\"stock\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.precio").value("El precio no puede superar $999.999.999."));
        assertThat(CATALOG.last()).isNull();
    }

    @Test
    void validacionDelCatalogoSeReenvia() throws Exception {
        CATALOG.respond(400, "{\"status\":400,\"mensaje\":\"Error de validación\","
                + "\"detalles\":{\"nombre\":\"ya existe\"}}");
        mvc.perform(auth(post(URL)).contentType(MediaType.APPLICATION_JSON).content(NUEVO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.nombre").value("ya existe"));
    }

    @Test
    void bodyIlegibleDa400() throws Exception {
        mvc.perform(auth(post(URL)).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").isNotEmpty());
    }

    @Test
    void catalogoRechazaTokenSePropaga401Y403() throws Exception {
        CATALOG.respond(401, "{}");
        mvc.perform(auth(get(URL))).andExpect(status().isUnauthorized());
        CATALOG.respond(403, "{}");
        mvc.perform(auth(get(URL))).andExpect(status().isForbidden());
    }

    @Test
    void error5xxDelCatalogoDa502SinFiltrarDetalles() throws Exception {
        CATALOG.respond(500, "{\"mensaje\":\"NullPointerException en ProductoService.java:42\"}");
        String body = mvc.perform(auth(get(URL)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.mensaje").value("Servicio de catálogo no disponible"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("NullPointer");
    }

    @Test
    void timeoutDelCatalogoDa503() throws Exception {
        CATALOG.respond(200, "[]");
        CATALOG.delay(4000);
        mvc.perform(auth(get(URL)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Servicio de catálogo no disponible"));
    }

    @Test
    void respuestaIlegibleDelCatalogoDa502() throws Exception {
        CATALOG.respond(200, "esto no es json");
        mvc.perform(auth(get(URL))).andExpect(status().isBadGateway());
    }
}
