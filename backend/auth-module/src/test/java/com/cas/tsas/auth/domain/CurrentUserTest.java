package com.cas.tsas.auth.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void constructor_rejects_null_id() {
        assertThatThrownBy(() -> new CurrentUser(null, Set.of(Role.COACH)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_rejects_null_roles() {
        assertThatThrownBy(() -> new CurrentUser(UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void roles_set_is_defensive_copy() {
        Set<Role> mutable = new HashSet<>();
        mutable.add(Role.COACH);
        CurrentUser user = new CurrentUser(UUID.randomUUID(), mutable);
        mutable.add(Role.ADMIN);
        assertThat(user.roles()).containsExactly(Role.COACH);
    }
}
