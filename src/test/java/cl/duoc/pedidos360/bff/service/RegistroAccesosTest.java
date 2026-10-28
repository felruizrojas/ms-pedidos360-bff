package cl.duoc.pedidos360.bff.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import tools.jackson.databind.json.JsonMapper;

class RegistroAccesosTest {

    @TempDir
    Path dir;

    @Test
    void elRegistroSobreviveAUnReinicio() {
        String archivo = dir.resolve("data/accesos.json").toString();
        RegistroAccesos registro = new RegistroAccesos(JsonMapper.builder().build(), archivo);
        registro.registrar("u-1", "Ana", "ana@contoso.cl");

        RegistroAccesos reiniciado = new RegistroAccesos(JsonMapper.builder().build(), archivo);
        assertThat(reiniciado.buscar("u-1")).hasValueSatisfying(a -> {
            assertThat(a.nombre()).isEqualTo("Ana");
            assertThat(a.primerAcceso()).isNotNull();
        });
        assertThat(reiniciado.buscar("u-2")).isEmpty();
    }

    @Test
    void sinArchivoQuedaEnMemoriaYConservaElPrimerAcceso() throws Exception {
        RegistroAccesos registro = new RegistroAccesos(JsonMapper.builder().build(), "");
        registro.registrar("u-1", "Ana", null);
        var primero = registro.buscar("u-1").orElseThrow();
        Thread.sleep(5);
        registro.registrar("u-1", "Ana", null);
        var segundo = registro.buscar("u-1").orElseThrow();
        assertThat(segundo.primerAcceso()).isEqualTo(primero.primerAcceso());
        assertThat(segundo.ultimoAcceso()).isAfter(primero.ultimoAcceso());
    }
}
