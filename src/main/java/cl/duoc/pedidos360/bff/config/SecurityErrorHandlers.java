package cl.duoc.pedidos360.bff.config;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.bff.exception.ErrorBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorHandlers {

    private final ObjectMapper objectMapper;

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            log.warn("401 en {}: {}", request.getRequestURI(), ex.getMessage());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            write(request, response, HttpStatus.UNAUTHORIZED, "Token ausente, inválido o expirado");
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            log.warn("403 en {} {}", request.getMethod(), request.getRequestURI());
            write(request, response, HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación");
        };
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status, String mensaje)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ErrorBody.of(status, mensaje, request.getRequestURI())));
    }
}
