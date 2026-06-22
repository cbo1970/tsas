package com.cas.tsas.auth.infrastructure.persistence;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.auth.domain.CurrentUser;
import com.cas.tsas.auth.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserAuditorTest {

    private final CurrentUserProvider provider = mock(CurrentUserProvider.class);
    private final CurrentUserAuditor auditor = new CurrentUserAuditor(provider);

    @Test
    void returns_current_user_id_when_present() {
        UUID userId = UUID.randomUUID();
        when(provider.get()).thenReturn(new CurrentUser(userId, Set.of(Role.COACH)));

        Optional<UUID> result = auditor.getCurrentAuditor();

        assertThat(result).contains(userId);
    }

    @Test
    void returns_empty_when_no_auth_context() {
        when(provider.get()).thenThrow(new IllegalStateException("No authenticated JWT"));

        Optional<UUID> result = auditor.getCurrentAuditor();

        assertThat(result).isEmpty();
    }
}
