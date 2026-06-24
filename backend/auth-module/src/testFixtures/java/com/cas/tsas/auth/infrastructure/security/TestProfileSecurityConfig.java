package com.cas.tsas.auth.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive {@code SecurityFilterChain} used only under the {@code test} profile so
 * integration tests can hit the API without Keycloak. Lives in {@code src/test/java}
 * (TEN-57 / STRIDE E2) so it is never packaged into the boot jar — eliminating the
 * risk that someone accidentally activates {@code SPRING_PROFILES_ACTIVE=test} in
 * production and ends up with a {@code permitAll} API. The complementary
 * {@link TestProfileGuard} fails the boot if the {@code test} profile is requested
 * in a production-like runtime.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestProfileSecurityConfig {

    private static final Logger LOG = LoggerFactory.getLogger(TestProfileSecurityConfig.class);

    @PostConstruct
    void warnPermitAllActive() {
        LOG.warn("=== TSaS TEST PROFILE ACTIVE — SecurityFilterChain is permitAll. "
                + "If you see this in a production log, STOP THE PROCESS. ===");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
