package com.cas.tsas.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void maps_realm_access_roles_to_ROLE_authorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub")
                .claim("realm_access", Map.of("roles", List.of("COACH", "ADMIN")))
                .build();

        List<String> authorities = converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority).toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_COACH", "ROLE_ADMIN");
    }

    @Test
    void returns_empty_when_realm_access_missing() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub").build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void returns_empty_when_roles_missing() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub").claim("realm_access", Map.of()).build();
        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void returns_empty_when_roles_is_not_a_collection() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .subject("sub").claim("realm_access", Map.of("roles", "not-a-list")).build();
        assertThat(converter.convert(jwt)).isEmpty();
    }
}
