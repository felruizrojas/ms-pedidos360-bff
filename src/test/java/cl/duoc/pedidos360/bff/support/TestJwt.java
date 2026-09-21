package cl.duoc.pedidos360.bff.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/** Genera JWT reales firmados con una clave RSA local (sin Azure). */
public final class TestJwt {

    public static final String ISSUER = "https://login.microsoftonline.com/test-tenant/v2.0";
    public static final String AUDIENCE = "test-api-client-id";

    private static final KeyPair KEYS = generate();
    private static final KeyPair OTHER_KEYS = generate();

    private String issuer = ISSUER;
    private String audience = AUDIENCE;
    private String scope = "access_as_user";
    private List<String> roles = List.of();
    private Instant expiresAt = Instant.now().plusSeconds(3600);
    private Instant notBefore = Instant.now().minusSeconds(60);
    private KeyPair signingKeys = KEYS;

    public static TestJwt valid() {
        return new TestJwt();
    }

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEYS.getPublic();
    }

    public TestJwt issuer(String v) { this.issuer = v; return this; }
    public TestJwt audience(String v) { this.audience = v; return this; }
    public TestJwt scope(String v) { this.scope = v; return this; }
    public TestJwt roles(String... v) { this.roles = List.of(v); return this; }
    public TestJwt expiresAt(Instant v) { this.expiresAt = v; return this; }
    public TestJwt notBefore(Instant v) { this.notBefore = v; return this; }
    public TestJwt signedWithOtherKey() { this.signingKeys = OTHER_KEYS; return this; }

    public String bearer() {
        return "Bearer " + build();
    }

    public String build() {
        try {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject("usuario-test")
                    .issueTime(Date.from(Instant.now()))
                    .notBeforeTime(Date.from(notBefore))
                    .expirationTime(Date.from(expiresAt));
            if (scope != null) {
                claims.claim("scp", scope);
            }
            if (!roles.isEmpty()) {
                claims.claim("roles", roles);
            }
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims.build());
            jwt.sign(new RSASSASigner(signingKeys.getPrivate()));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyPair generate() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
