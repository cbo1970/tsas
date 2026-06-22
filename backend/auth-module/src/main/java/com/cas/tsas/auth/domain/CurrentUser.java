package com.cas.tsas.auth.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Aktueller Nutzer aus dem JWT (`sub` + Realm-Rollen). Pure POJO. */
public record CurrentUser(UUID id, Set<Role> roles) {

    public CurrentUser {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
