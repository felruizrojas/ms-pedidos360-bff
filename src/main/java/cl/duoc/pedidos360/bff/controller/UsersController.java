package cl.duoc.pedidos360.bff.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.dto.AccesoDto;
import cl.duoc.pedidos360.bff.dto.UsuarioDto;
import cl.duoc.pedidos360.bff.service.RegistroAccesos;
import cl.duoc.pedidos360.bff.service.UsuariosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

/** Usuarios de la aplicación (Entra ID vía Microsoft Graph). Solo lectura. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsuariosService usuariosService;
    private final RegistroAccesos registroAccesos;

    @Operation(summary = "Listar usuarios con App role en la API",
            description = "Datos de Entra ID más el estado de inicio de sesión (activo = entró al menos una vez). Solo Admin.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuarios"),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Falta el scope o el rol Admin"),
        @ApiResponse(responseCode = "502", description = "Microsoft Graph rechazó la consulta (secret o permisos)"),
        @ApiResponse(responseCode = "503", description = "Microsoft Graph no disponible o no configurado")
    })
    @GetMapping
    public List<UsuarioDto> listar() {
        return usuariosService.listar();
    }

    @Operation(summary = "Registrar el inicio de sesión del usuario que llama",
            description = "El front lo llama al iniciar sesión; cualquier request autenticado también registra el acceso.")
    @GetMapping("/me")
    public AccesoDto yo(@AuthenticationPrincipal Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return registroAccesos.buscar(oid != null ? oid : "")
                .map(a -> new AccesoDto(a.id(), a.nombre(), a.email(), a.primerAcceso(), a.ultimoAcceso()))
                .orElseGet(() -> new AccesoDto(oid, jwt.getClaimAsString("name"), null, null, null));
    }
}
