package com.cas.tsas.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    @Test
    void hasRole_returns_true_when_role_present() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.COACH, Role.ADMIN));
        assertThat(user.hasRole(Role.ADMIN)).isTrue();
        assertThat(user.hasRole(Role.COACH)).isTrue();
    }

    @Test
    void hasRole_returns_false_when_role_absent() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), Set.of(Role.COACH));
        assertThat(user.hasRole(Role.ADMIN)).isFalse();
    }
}
