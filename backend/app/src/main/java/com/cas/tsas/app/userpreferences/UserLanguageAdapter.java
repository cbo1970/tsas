package com.cas.tsas.app.userpreferences;

import com.cas.tsas.ai.application.port.out.UserLanguagePort;
import org.springframework.stereotype.Component;

/** Adapter, der den ai-module-{@link UserLanguagePort} mit dem App-internen {@link UserPreferenceService} verbindet (TEN-6). */
@Component
public class UserLanguageAdapter implements UserLanguagePort {

    private final UserPreferenceService userPreferenceService;

    public UserLanguageAdapter(UserPreferenceService userPreferenceService) {
        this.userPreferenceService = userPreferenceService;
    }

    @Override
    public String currentLanguage() {
        return userPreferenceService.getCurrentLanguage();
    }
}
