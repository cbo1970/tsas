package com.cas.tsas.app.userpreferences;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA-Entity für die Sprachpräferenz eines Nutzers (TEN-6). */
@Entity
@Table(name = "user_preferences")
public class UserPreferenceJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "language", nullable = false, length = 2)
    private String language;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserPreferenceJpaEntity() {}

    public UserPreferenceJpaEntity(UUID userId, String language) {
        this.userId = userId;
        this.language = language;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public String getLanguage() { return language; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setLanguage(String language) {
        this.language = language;
        this.updatedAt = Instant.now();
    }
}
