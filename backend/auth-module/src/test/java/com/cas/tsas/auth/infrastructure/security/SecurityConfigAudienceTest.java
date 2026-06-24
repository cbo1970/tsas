package com.cas.tsas.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the audience validator from {@link SecurityConfig#audienceValidator(String)} —
 * exercises the {@code aud}-claim check without spinning up a {@code NimbusJwtDecoder} (the real
 * decoder requires a reachable JWK Set URL).
 */
class SecurityConfigAudienceTest {

    private final OAuth2TokenValidator<Jwt> validator =
            SecurityConfig.audienceValidator("tsas-frontend");

    @Test
    void accepts_token_with_matching_audience() {
        Jwt jwt = jwt(List.of("tsas-frontend"));
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertFalse(result.hasErrors(), () -> "expected no errors but got: " + result.getErrors());
    }

    @Test
    void accepts_token_whose_audience_list_contains_expected_value_among_others() {
        Jwt jwt = jwt(List.of("tsas-frontend", "some-other-client"));
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertFalse(result.hasErrors());
    }

    @Test
    void rejects_token_with_different_audience() {
        Jwt jwt = jwt(List.of("evil-other-client"));
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
    }

    @Test
    void rejects_token_with_empty_audience_list() {
        Jwt jwt = jwt(List.of());
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
    }

    @Test
    void rejects_token_without_audience_claim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertTrue(result.hasErrors());
    }

    private static Jwt jwt(List<String> audience) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .audience(audience)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
