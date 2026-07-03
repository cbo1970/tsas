package com.cas.tsas.auth.infrastructure.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Refuses to boot the application when the {@code test} profile is requested
 * in a production-like runtime (TEN-57 / STRIDE E2). The {@code test} profile
 * activates a {@code permitAll} SecurityFilterChain — accidentally enabling it
 * in production would open every endpoint.
 *
 * <p>The guard runs as an {@link EnvironmentPostProcessor}, i.e. before the
 * application context is built. Two checks:
 * <ol>
 *   <li><b>Profile combination:</b> {@code test} together with any of
 *       {@code prod}, {@code production}, {@code docker} fails — these names
 *       are conventional for prod runtimes and never make sense alongside
 *       {@code test}.</li>
 *   <li><b>Datasource sanity:</b> if {@code test} is active but
 *       {@code spring.datasource.url} is not an in-memory H2 URL, the runtime
 *       is talking to a real database (PostgreSQL etc.). That is a near-certain
 *       configuration error — the permitAll chain must not run against a real
 *       database.</li>
 * </ol>
 *
 * Registered via {@code META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports}.
 */
public class TestProfileGuard implements EnvironmentPostProcessor, Ordered {

    static final String TEST_PROFILE = "test";

    static final Set<String> PROD_LIKE_PROFILES = Set.of("prod", "production", "docker");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        List<String> active = Arrays.asList(env.getActiveProfiles());
        if (!active.contains(TEST_PROFILE)) {
            return;
        }

        List<String> conflicting = active.stream()
                .filter(PROD_LIKE_PROFILES::contains)
                .toList();
        if (!conflicting.isEmpty()) {
            throw new IllegalStateException(
                    "Refusing to start: 'test' profile must not be combined with "
                            + conflicting
                            + ". The 'test' profile activates a permitAll SecurityFilterChain "
                            + "(TestProfileSecurityConfig) — never enable it in production.");
        }

        String dsUrl = env.getProperty("spring.datasource.url", "");
        if (!isInMemoryH2(dsUrl)) {
            throw new IllegalStateException(
                    "Refusing to start: 'test' profile is active but spring.datasource.url='"
                            + dsUrl
                            + "' is not an in-memory H2 URL. The permitAll chain must not run "
                            + "against a real database. Unset SPRING_PROFILES_ACTIVE=test in "
                            + "production deployments.");
        }
    }

    private static boolean isInMemoryH2(String dsUrl) {
        return dsUrl.startsWith("jdbc:h2:mem:");
    }

    @Override
    public int getOrder() {
        // Run after Spring Boot's ConfigDataEnvironmentPostProcessor so application-test.yml
        // (with its in-memory H2 datasource) has already been merged into the environment.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
