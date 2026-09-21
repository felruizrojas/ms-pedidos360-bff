package cl.duoc.pedidos360.bff.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;

/**
 * Decoder explícito: firma (JWKS del issuer), exp/nbf, issuer y audience. Se define a mano para que
 * la validación de audience no dependa de la autoconfiguración.
 */
@Configuration
public class JwtConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") List<String> audiences) {
        OAuth2TokenValidator<Jwt> validator = validator(issuer, audiences);
        // Lazy: el descubrimiento OIDC/JWKS ocurre en el primer token, no al arrancar.
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            decoder.setJwtValidator(validator);
            return decoder;
        });
    }

    public static OAuth2TokenValidator<Jwt> validator(String issuer, List<String> audiences) {
        List<String> esperadas = audiences.stream().map(String::trim).filter(a -> !a.isEmpty()).toList();
        if (esperadas.isEmpty()) {
            throw new IllegalStateException("Debe configurarse al menos una audience (ENTRA_API_CLIENT_ID)");
        }
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> aud = jwt.getAudience();
            if (aud != null && !Collections.disjoint(aud, esperadas)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Audience no autorizada", null));
        };
        // createDefaultWithIssuer = timestamp (exp/nbf) + issuer
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience);
    }
}
