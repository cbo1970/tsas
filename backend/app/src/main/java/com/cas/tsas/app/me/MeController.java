package com.cas.tsas.app.me;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DSGVO-Endpunkte (TEN-66): Datenexport (Art. 20) und Recht auf Löschung (Art. 17) für
 * den aktuell authentifizierten Nutzer. Beide Operationen leiten den Owner aus dem JWT
 * ab — kein User-Parameter, kein Cross-Tenant-Zugriff.
 */
@RestController
@RequestMapping("/api/me")
public class MeController {

    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    /** Art. 20 — Datenübertragbarkeit. JSON-Snapshot aller eigenen Aggregate. */
    @GetMapping(value = "/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public UserDataExport export() {
        return meService.exportCurrentUserData();
    }

    /** Art. 17 — Recht auf Löschung. Entfernt alle eigenen Aggregate; gibt die gelöschten Counts zurück. */
    @DeleteMapping
    public MeService.DeletionSummary delete() {
        return meService.deleteCurrentUserData();
    }
}
