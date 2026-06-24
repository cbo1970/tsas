package com.cas.tsas.auth.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * OAuth2 resource-server security for all non-test profiles. Validates Keycloak-issued JWTs
 * (issuer + JWK set + audience), maps `realm_access.roles` to `ROLE_*` Spring authorities,
 * requires authentication for every request except the Actuator health/info and OpenAPI
 * endpoints, and disables CSRF for the stateless API.
 *
 * <p>The audience check (TEN-56 / STRIDE S1) rejects tokens issued for other realm clients —
 * without it, any token from the {@code tsas} realm would be accepted regardless of client.
 * The audience claim is supplied by Keycloak's {@code oidc-audience-mapper} on the
 * {@code tsas-frontend} client (see {@code docker/keycloak/realm-export.json}); the expected
 * value is configurable via {@code tsas.security.expected-audience} so a future migration
 * (e.g. an additional API-client) does not require a code change.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${tsas.security.expected-audience:tsas-frontend}") String expectedAudience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audience = audienceValidator(expectedAudience);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audience));
        return decoder;
    }

    /**
     * Builds a {@link JwtClaimValidator} for the {@code aud} claim — exposed package-private so
     * a unit test can exercise the validator without standing up a full {@link NimbusJwtDecoder}.
     */
    static OAuth2TokenValidator<Jwt> audienceValidator(String expectedAudience) {
        return new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains(expectedAudience));
    }
}
