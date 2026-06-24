package com.cas.tsas.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit-tests for {@link TestProfileGuard} (TEN-57 / STRIDE E2). */
class TestProfileGuardTest {

    private final TestProfileGuard guard = new TestProfileGuard();
    private final SpringApplication app = new SpringApplication();

    @Test
    void allows_no_profile_at_all() {
        MockEnvironment env = new MockEnvironment();
        assertDoesNotThrow(() -> guard.postProcessEnvironment(env, app));
    }

    @Test
    void allows_test_profile_with_h2_in_memory_datasource() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:tsasdb;DB_CLOSE_DELAY=-1");
        env.setActiveProfiles("test");
        assertDoesNotThrow(() -> guard.postProcessEnvironment(env, app));
    }

    @Test
    void allows_local_profile_with_postgres() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/tsas");
        env.setActiveProfiles("local");
        assertDoesNotThrow(() -> guard.postProcessEnvironment(env, app));
    }

    @Test
    void rejects_test_plus_prod() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:tsasdb");
        env.setActiveProfiles("test", "prod");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard.postProcessEnvironment(env, app));
        assertTrue(ex.getMessage().contains("'test' profile must not be combined"),
                () -> "unexpected message: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("prod"));
    }

    @Test
    void rejects_test_plus_docker() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:tsasdb");
        env.setActiveProfiles("test", "docker");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard.postProcessEnvironment(env, app));
        assertTrue(ex.getMessage().contains("docker"));
    }

    @Test
    void rejects_test_plus_production() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:h2:mem:tsasdb");
        env.setActiveProfiles("test", "production");
        assertThrows(IllegalStateException.class,
                () -> guard.postProcessEnvironment(env, app));
    }

    @Test
    void rejects_test_profile_with_postgres_datasource() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/tsas");
        env.setActiveProfiles("test");
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> guard.postProcessEnvironment(env, app));
        assertTrue(ex.getMessage().contains("not an in-memory H2 URL"),
                () -> "unexpected message: " + ex.getMessage());
    }

    @Test
    void rejects_test_profile_with_empty_datasource_url() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        // No spring.datasource.url at all → also not an H2 URL → reject.
        assertThrows(IllegalStateException.class,
                () -> guard.postProcessEnvironment(env, app));
    }
}
