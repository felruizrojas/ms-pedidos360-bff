package cl.duoc.pedidos360.bff.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=" + TestJwt.ISSUER,
        "spring.security.oauth2.resourceserver.jwt.audiences=" + TestJwt.AUDIENCE,
        "pedidos360.services.read-timeout=1500ms",
        "pedidos360.services.connect-timeout=500ms"
})
@AutoConfigureMockMvc
@Import(TestJwtDecoderConfig.class)
public abstract class AbstractBffTest {

    protected static final FakeCatalogServer CATALOG = new FakeCatalogServer();

    @DynamicPropertySource
    static void catalogUrl(DynamicPropertyRegistry registry) {
        registry.add("pedidos360.services.catalog.base-url", CATALOG::baseUrl);
    }
}
