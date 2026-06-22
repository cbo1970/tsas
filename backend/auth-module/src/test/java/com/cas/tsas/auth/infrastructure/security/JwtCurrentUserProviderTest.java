package com.cas.tsas.auth.infrastructure.security;

import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtCurrentUserProviderTest {

    private final JwtCurrentUserProvider provider = new JwtCurrentUserProvider();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void extracts_sub_and_role_authorities() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t")
                .header("alg", "none")
                .subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_COACH"),
                        new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        CurrentUser user = provider.get();

        assertThat(user.id()).isEqualTo(sub);
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.COACH, Role.ADMIN);
    }

    @Test
    void throws_when_no_authentication() {
        assertThatThrownBy(provider::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void throws_when_authentication_is_anonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("k", "anonymous",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        assertThatThrownBy(provider::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ignores_authorities_without_ROLE_prefix() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt,
                        List.of(new SimpleGrantedAuthority("SCOPE_email"),
                                new SimpleGrantedAuthority("ROLE_COACH"))));

        CurrentUser user = provider.get();

        assertThat(user.roles()).containsExactly(Role.COACH);
    }

    @Test
    void ignores_unknown_role_names() {
        UUID sub = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject(sub.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt,
                        List.of(new SimpleGrantedAuthority("ROLE_UNKNOWN"),
                                new SimpleGrantedAuthority("ROLE_ADMIN"))));

        CurrentUser user = provider.get();

        assertThat(user.roles()).containsExactly(Role.ADMIN);
    }

    @Test
    void throws_when_sub_is_not_a_uuid() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .subject("not-a-uuid")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of()));

        assertThatThrownBy(provider::get).isInstanceOf(IllegalStateException.class);
    }
}
