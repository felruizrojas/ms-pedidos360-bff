package cl.duoc.pedidos360.bff.support;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import cl.duoc.pedidos360.bff.config.JwtConfig;

/** Reemplaza solo la fuente de claves (JWKS de Entra -> clave local); las validaciones son las de producción. */
@TestConfiguration
public class TestJwtDecoderConfig {

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestJwt.publicKey()).build();
        decoder.setJwtValidator(JwtConfig.validator(TestJwt.ISSUER, List.of(TestJwt.AUDIENCE)));
        return decoder;
    }
}
