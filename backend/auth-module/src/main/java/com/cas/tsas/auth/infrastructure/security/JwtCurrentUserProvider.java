package com.cas.tsas.auth.infrastructure.security;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtCurrentUserProvider.class);
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("No authenticated JWT in security context");
        }
        String subject = jwtAuth.getToken().getSubject();
        UUID id;
        try {
            id = UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT sub is not a UUID: " + subject, e);
        }
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (GrantedAuthority ga : jwtAuth.getAuthorities()) {
            String raw = ga.getAuthority();
            if (raw.startsWith(ROLE_PREFIX)) {
                try {
                    roles.add(Role.valueOf(raw.substring(ROLE_PREFIX.length())));
                } catch (IllegalArgumentException e) {
                    log.debug("Ignoring unknown role authority: {}", raw);
                }
            }
        }
        return new CurrentUser(id, roles);
    }
}
