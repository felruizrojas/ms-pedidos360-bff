package cl.duoc.pedidos360.bff.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Quién entró a la aplicación: cada request autenticado registra al usuario (por su oid de Entra ID, el mismo
 * id que Graph) con su primer y último acceso. Con pedidos360.accesos.archivo se guarda en un JSON para que
 * sobreviva a reinicios; vacío = solo en memoria. El último acceso se escribe a disco como mucho una vez
 * por minuto y usuario, para no escribir en cada request.
 */
@Slf4j
@Service
public class RegistroAccesos {

    private static final Duration INTERVALO_GUARDADO = Duration.ofMinutes(1);

    public record Acceso(String id, String nombre, String email, Instant primerAcceso, Instant ultimoAcceso) {
    }

    private final Map<String, Acceso> accesos = new ConcurrentHashMap<>();
    private final Map<String, Instant> guardadoEn = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final Path archivo;

    public RegistroAccesos(ObjectMapper objectMapper, @Value("${pedidos360.accesos.archivo:}") String archivo) {
        this.objectMapper = objectMapper;
        this.archivo = archivo == null || archivo.isBlank() ? null : Path.of(archivo);
        cargar();
    }

    public void registrar(String id, String nombre, String email) {
        Instant ahora = Instant.now();
        Acceso previo = accesos.get(id);
        accesos.put(id, new Acceso(id, nombre, email, previo != null ? previo.primerAcceso() : ahora, ahora));
        Instant ultimoGuardado = guardadoEn.get(id);
        if (previo == null || ultimoGuardado == null || ultimoGuardado.plus(INTERVALO_GUARDADO).isBefore(ahora)) {
            guardadoEn.put(id, ahora);
            guardar();
        }
    }

    public Optional<Acceso> buscar(String id) {
        return Optional.ofNullable(accesos.get(id));
    }

    private void cargar() {
        if (archivo == null || !Files.exists(archivo)) {
            return;
        }
        try {
            List<Acceso> guardados = objectMapper.readValue(archivo.toFile(), new TypeReference<List<Acceso>>() { });
            guardados.forEach(a -> accesos.put(a.id(), a));
            log.info("Registro de accesos: {} usuarios cargados desde {}", accesos.size(), archivo);
        } catch (RuntimeException e) {
            log.error("No se pudo leer el registro de accesos {}; se parte vacío", archivo, e);
        }
    }

    private synchronized void guardar() {
        if (archivo == null) {
            return;
        }
        try {
            if (archivo.getParent() != null) {
                Files.createDirectories(archivo.getParent());
            }
            Path temporal = archivo.resolveSibling(archivo.getFileName() + ".tmp");
            objectMapper.writeValue(temporal.toFile(), List.copyOf(accesos.values()));
            Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException e) {
            log.error("No se pudo guardar el registro de accesos en {}", archivo, e);
        }
    }
}
