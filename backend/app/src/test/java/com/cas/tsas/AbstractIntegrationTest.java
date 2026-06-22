package com.cas.tsas;

import com.cas.tsas.auth.domain.Role;
import com.cas.tsas.auth.testsupport.JwtTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Base for full-stack integration tests. Starts a shared PostgreSQL Testcontainer once per JVM.
 * Intentionally loads the non-local Spring profile (SecurityConfig with JWT), relying on
 * NimbusJwtDecoder's lazy JWKS fetch and the jwt() post-processor to avoid needing a live Keycloak.
 *
 * <p>Every MockMvc request is pre-populated with a default JWT post-processor whose {@code sub} is
 * {@link #DEFAULT_USER} and whose realm role is {@link Role#COACH}. This means {@code
 * CurrentUserProvider.get()} returns a valid {@link com.cas.tsas.auth.domain.CurrentUser} for the
 * fixed default user in every test, and fixture data persisted via the REST API is owned by that
 * same user. Tests that need a different identity can override per-request with {@code
 * .with(JwtTestSupport.withUser(otherId, Role.COACH))} on the specific call.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    /** Fixed UUID used as the default JWT {@code sub} (and therefore owner_id) for all IT requests. */
    protected static final UUID DEFAULT_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    static final PostgreSQLContainer POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private WebApplicationContext wac;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .defaultRequest(get("/").with(JwtTestSupport.withUser(DEFAULT_USER, Role.COACH)))
                .build();
    }
}
