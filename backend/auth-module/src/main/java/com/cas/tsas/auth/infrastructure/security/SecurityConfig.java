package com.cas.tsas.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration stub.
 * Currently permits all requests to allow local development without Keycloak.
 * <p>
 * TODO (V1): Configure OAuth2 resource server with Keycloak JWT validation.
 *   - Uncomment `.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))`
 *   - Set `spring.security.oauth2.resourceserver.jwt.issuer-uri` in application.yml
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configure(http))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        // TODO: enable JWT resource server once Keycloak is set up
        // .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
