package cl.duoc.pedidos360.bff.service;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.bff.client.GraphClient;
import cl.duoc.pedidos360.bff.client.UsuarioGraph;
import cl.duoc.pedidos360.bff.dto.UsuarioDto;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuariosService {

    private static final Collator COLLATOR = Collator.getInstance(Locale.forLanguageTag("es-CL"));

    private final GraphClient graphClient;
    private final RegistroAccesos registroAccesos;

    /** Usuarios con App role en la API, ordenados por nombre, con su estado de inicio de sesión. */
    public List<UsuarioDto> listar() {
        return graphClient.usuariosConRol().stream()
                .map(this::conAcceso)
                .sorted(Comparator.comparing(u -> Objects.requireNonNullElse(u.nombre(), ""), COLLATOR))
                .toList();
    }

    private UsuarioDto conAcceso(UsuarioGraph u) {
        var acceso = registroAccesos.buscar(u.id());
        return new UsuarioDto(u.id(), u.nombre(), u.nombres(), u.apellidos(), u.email(), u.usuarioPrincipal(),
                u.roles(), u.cuentaHabilitada(), u.tipo(), u.cargo(), u.departamento(), u.telefono(), u.creadoEn(),
                acceso.isPresent(),
                acceso.map(RegistroAccesos.Acceso::primerAcceso).orElse(null),
                acceso.map(RegistroAccesos.Acceso::ultimoAcceso).orElse(null));
    }
}
