package cl.duoc.pedidos360.bff.client;

import java.time.Instant;
import java.util.List;

/** Usuario de Entra ID con sus App roles en la API, tal como lo devuelve Microsoft Graph. */
public record UsuarioGraph(
        String id,
        String nombre,
        String nombres,
        String apellidos,
        String email,
        String usuarioPrincipal,
        Boolean cuentaHabilitada,
        String tipo,
        String cargo,
        String departamento,
        String telefono,
        Instant creadoEn,
        List<String> roles) {
}
