package com.cas.tsas.ai.application.port.out;

/**
 * TEN-6 — Out-Port: liefert die Sprachpräferenz des aktuellen Nutzers (de|en|it|fr). Wird vom
 * {@code MatchAnalysisService} und {@code OpponentPreparationService} aufgerufen, damit die
 * LLM-Antworten in der vom Nutzer gewählten Sprache zurückkommen. Der Adapter lebt im
 * {@code app}-Modul (Zugriff auf die DB-gestützte Persistenz), damit das ai-module nicht direkt
 * an die Persistenz-Schicht koppelt.
 */
public interface UserLanguagePort {

    /**
     * @return ISO-Sprachcode des aktuellen Nutzers, fällt auf {@code "de"} zurück, wenn nichts
     *     gespeichert ist oder kein authentifizierter Nutzer im Kontext steht.
     */
    String currentLanguage();
}
