package com.cas.tsas.app.userpreferences;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Liest / schreibt die Sprachpräferenz des aktuellen Nutzers (TEN-6).
 * Verträgt sich mit allen Nutzern, auch wenn noch kein DB-Eintrag existiert: {@link #getLanguage(UUID)}
 * liefert den Default {@link #DEFAULT_LANGUAGE} = "de".
 */
@Service
public class UserPreferenceService {

    public static final String DEFAULT_LANGUAGE = "de";
    public static final Set<String> SUPPORTED_LANGUAGES = Set.of("de", "en", "it", "fr");

    private final UserPreferenceJpaRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public UserPreferenceService(UserPreferenceJpaRepository repository,
                                 CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    /** Aktuelle Sprache des Anfragenden — fällt auf 'de' zurück, wenn nichts gespeichert ist. */
    @Transactional(readOnly = true)
    public String getCurrentLanguage() {
        return getLanguage(currentUserProvider.get().id());
    }

    /** Sprache eines beliebigen Users (für Backend-Services wie das LLM-Prompting). */
    @Transactional(readOnly = true)
    public String getLanguage(UUID userId) {
        return repository.findById(userId)
                .map(UserPreferenceJpaEntity::getLanguage)
                .orElse(DEFAULT_LANGUAGE);
    }

    /** Sprache des aktuellen Nutzers persistieren. Wirft {@link IllegalArgumentException} für unbekannte Codes. */
    @Transactional
    public String updateLanguage(String language) {
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new IllegalArgumentException(
                    "Unbekannter Sprachcode '" + language + "'. Unterstützt: " + SUPPORTED_LANGUAGES);
        }
        UUID userId = currentUserProvider.get().id();
        UserPreferenceJpaEntity entity = repository.findById(userId)
                .orElseGet(() -> new UserPreferenceJpaEntity(userId, language));
        entity.setLanguage(language);
        repository.save(entity);
        return language;
    }
}
