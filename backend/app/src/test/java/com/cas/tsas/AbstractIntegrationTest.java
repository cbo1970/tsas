package com.cas.tsas;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Base for full-stack integration tests. Starts a shared PostgreSQL Testcontainer once per JVM.
 * Intentionally loads the non-local Spring profile (SecurityConfig with JWT), relying on
 * NimbusJwtDecoder's lazy JWKS fetch and the jwt() post-processor to avoid needing a live Keycloak.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

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
                .defaultRequest(get("/").with(jwt()))
                .build();
    }
}
