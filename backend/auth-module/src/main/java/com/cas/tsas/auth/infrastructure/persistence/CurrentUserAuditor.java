package com.cas.tsas.auth.infrastructure.persistence;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Liefert für JPA-Auditing die UUID des aktuell authentifizierten Nutzers. Wenn kein
 * Auth-Kontext vorhanden ist (Flyway-Migration, scheduled Job, DataJpaTest ohne MockMvc),
 * wird ein leeres Optional zurückgegeben — Hibernate lässt die Audit-Spalten dann auf NULL.
 */
@Component("currentUserAuditor")
public class CurrentUserAuditor implements AuditorAware<UUID> {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserAuditor(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public Optional<UUID> getCurrentAuditor() {
        try {
            return Optional.of(currentUserProvider.get().id());
        } catch (IllegalStateException e) {
            return Optional.empty();
        }
    }
}
