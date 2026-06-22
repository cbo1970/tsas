package com.cas.tsas.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Aktiviert JPA-Auditing: Spring Data füllt {@code @CreatedDate}, {@code @CreatedBy},
 * {@code @LastModifiedDate} und {@code @LastModifiedBy} auf den Entities automatisch.
 * Auditor-Quelle ist {@link com.cas.tsas.auth.infrastructure.persistence.CurrentUserAuditor}.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUserAuditor")
public class JpaAuditingConfig {
}
