package cl.duoc.pedidos360.bff.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import cl.duoc.pedidos360.bff.exception.ErrorBody;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Exige que /api/** llegue desde el API Gateway: el Gateway agrega X-Origin-Verify con un secreto
 * compartido (parameter mapping) y aquí se compara. Así nadie puede saltarse el Gateway aunque tenga
 * un JWT válido. Con pedidos360.gateway.secret vacío (local, tests) el filtro no hace nada.
 * Corre antes de Spring Security: el request se rechaza sin llegar a validar el token.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class GatewayOriginFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Origin-Verify";

    private final byte[] secret;
    private final ObjectMapper objectMapper;

    public GatewayOriginFilter(@Value("${pedidos360.gateway.secret:}") String secret, ObjectMapper objectMapper) {
        this.secret = secret.trim().getBytes(StandardCharsets.UTF_8);
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return secret.length == 0 || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibido = request.getHeader(HEADER);
        // Comparación en tiempo constante para no filtrar el secreto por timing.
        if (recibido != null && MessageDigest.isEqual(secret, recibido.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("403 en {} {}: request sin pasar por el API Gateway", request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ErrorBody.of(HttpStatus.FORBIDDEN,
                "Acceso permitido solo a través del API Gateway", request.getRequestURI())));
    }
}
