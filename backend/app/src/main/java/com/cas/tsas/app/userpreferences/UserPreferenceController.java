package com.cas.tsas.app.userpreferences;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Endpoints für die Sprachpräferenz des aktuell authentifizierten Nutzers (TEN-6).
 * Owner wird aus dem JWT abgeleitet — kein Pfad-Parameter, kein Cross-Tenant-Zugriff.
 */
@RestController
@RequestMapping("/api/user-preferences")
@Validated
public class UserPreferenceController {

    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @GetMapping
    public UserPreferenceResponse get() {
        return new UserPreferenceResponse(service.getCurrentLanguage());
    }

    @PutMapping
    public ResponseEntity<UserPreferenceResponse> update(@RequestBody UpdateLanguageRequest request) {
        String saved = service.updateLanguage(request.language());
        return ResponseEntity.ok(new UserPreferenceResponse(saved));
    }

    public record UserPreferenceResponse(String language) {}

    public record UpdateLanguageRequest(
            @NotBlank
            @Pattern(regexp = "de|en|it|fr", message = "language must be one of de|en|it|fr")
            String language
    ) {}
}
