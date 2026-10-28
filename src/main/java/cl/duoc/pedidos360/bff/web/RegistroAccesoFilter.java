package cl.duoc.pedidos360.bff.web;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cl.duoc.pedidos360.bff.service.RegistroAccesos;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Registra el acceso de cada request a /api/** con JWT válido. Corre después de la cadena de Spring
 * Security (orden por defecto), así que solo ve tokens ya validados. Usa el claim "oid" (id del usuario
 * en Entra ID, el mismo que devuelve Graph); un token sin oid no se registra.
 */
@Component
@RequiredArgsConstructor
public class RegistroAccesoFilter extends OncePerRequestFilter {

    private final RegistroAccesos registroAccesos;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String oid = jwt.getClaimAsString("oid");
            if (oid != null && !oid.isBlank()) {
                String email = jwt.getClaimAsString("email");
                registroAccesos.registrar(oid, jwt.getClaimAsString("name"),
                        email != null ? email : jwt.getClaimAsString("preferred_username"));
            }
        }
        chain.doFilter(request, response);
    }
}
