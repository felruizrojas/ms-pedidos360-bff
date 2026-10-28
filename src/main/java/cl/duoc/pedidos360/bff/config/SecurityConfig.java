package cl.duoc.pedidos360.bff.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cl.duoc.pedidos360.bff.web.RequestIdFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String SCOPE = "SCOPE_access_as_user";
    private static final String ADMIN = "ROLE_Admin";
    private static final String OPERADOR = "ROLE_Operador";
    private static final String CLIENTE = "ROLE_Cliente";

    private final SecurityErrorHandlers errorHandlers;

    @Value("${pedidos360.security.enforce-roles:false}")
    private boolean enforceRoles;

    @Value("${pedidos360.cors.allowed-origins:}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/actuator/health").permitAll();
                    // Usuarios (datos personales de Entra ID): solo Admin, siempre, aunque ENFORCE_ROLES=false.
                    // /api/users/me queda para cualquier usuario con el scope (registra su inicio de sesión).
                    auth.requestMatchers(HttpMethod.GET, "/api/users").access(scopeY(ADMIN));
                    if (enforceRoles) {
                        // Catálogo: escribir y ver los eliminados (para restaurarlos) es solo de Admin.
                        auth.requestMatchers(HttpMethod.GET, "/api/catalog/products/eliminados")
                                .access(scopeY(ADMIN));
                        for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
                            auth.requestMatchers(method, "/api/catalog/**").access(scopeY(ADMIN));
                        }
                        // Pedidos: los actores son Operador y Cliente; borrar es solo del Operador. La autorización
                        // fina (dueño del pedido, qué transición puede hacer cada rol) la hace orders.
                        auth.requestMatchers(HttpMethod.DELETE, "/api/orders/**").access(scopeY(OPERADOR));
                        auth.requestMatchers("/api/orders/**").access(scopeY(OPERADOR, CLIENTE));
                    }
                    auth.requestMatchers("/api/**").hasAuthority(SCOPE)
                            .anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(errorHandlers.accessDeniedHandler()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(errorHandlers.authenticationEntryPoint())
                        .accessDeniedHandler(errorHandlers.accessDeniedHandler())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Exige el scope de la API y al menos uno de los roles. */
    private static AuthorizationManager<RequestAuthorizationContext> scopeY(String... roles) {
        return AuthorizationManagers.allOf(
                AuthorityAuthorizationManager.hasAuthority(SCOPE),
                AuthorityAuthorizationManager.hasAnyAuthority(roles));
    }

    /** scope -> SCOPE_x (por defecto) y claim "roles" (App roles de Entra) -> ROLE_x. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles != null) {
                roles.forEach(rol -> authorities.add(new SimpleGrantedAuthority("ROLE_" + rol)));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = corsAllowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty()).toList();
        if (!origins.isEmpty()) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(origins);
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", RequestIdFilter.HEADER));
            configuration.setExposedHeaders(List.of(RequestIdFilter.HEADER));
            source.registerCorsConfiguration("/**", configuration);
        }
        return source;
    }
}
