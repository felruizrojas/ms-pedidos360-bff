package cl.duoc.pedidos360.bff.dto;

import java.time.Instant;
import java.util.List;

/**
 * Usuario de la aplicación para el dashboard (solo lectura). Los datos vienen de Entra ID (Graph);
 * activo/primerAcceso/ultimoAcceso, del registro de accesos del BFF: activo = inició sesión al menos una vez.
 */
public record UsuarioDto(
        String id,
        String nombre,
        String nombres,
        String apellidos,
        String email,
        String usuarioPrincipal,
        List<String> roles,
        Boolean cuentaHabilitada,
        String tipo,
        String cargo,
        String departamento,
        String telefono,
        Instant creadoEn,
        boolean activo,
        Instant primerAcceso,
        Instant ultimoAcceso) {
}
