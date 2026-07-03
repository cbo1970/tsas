package com.cas.tsas.auth.testsupport;

import com.cas.tsas.auth.domain.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Test-Helper für MockMvc: produziert einen {@link JwtRequestPostProcessor} mit gesetztem
 * `sub`-Claim und `realm_access.roles` sowie passenden Spring-Authorities. Gedacht für
 * Cross-Tenant-Integrationstests, die ohne Keycloak laufen.
 *
 * <pre>{@code
 * mockMvc.perform(get("/api/players").with(JwtTestSupport.withUser(USER_A_ID, Role.COACH)))
 * }</pre>
 */
public final class JwtTestSupport {

    private JwtTestSupport() {}

    public static JwtRequestPostProcessor withUser(UUID userId, Role... roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.name()))
                .toList();
        return jwt()
                .jwt(j -> j
                        .subject(userId.toString())
                        .claim("realm_access", Map.of("roles", roleNames(roles))))
                .authorities(authorities);
    }

    private static List<String> roleNames(Role[] roles) {
        return Arrays.stream(roles).map(Role::name).toList();
    }
}
