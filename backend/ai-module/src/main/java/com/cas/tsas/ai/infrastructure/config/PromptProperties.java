package com.cas.tsas.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalisierte LLM-Prompt-Bausteine. Bindet {@code tsas.ai.prompt.*} aus der
 * Konfiguration; die {@link DefaultValue}-Texte dienen als sicherer Fallback und können per
 * {@code application.yml} oder Umgebungsvariable (z. B. {@code TSAS_AI_PROMPT_SYSTEM})
 * überschrieben werden — etwa zur Sprach- oder Tonalitätsanpassung ohne Neu-Build.
 *
 * @param system                    Rollen-/Aufgabenbeschreibung für die Postmortem-System-Message (FA-11).
 * @param userInstruction           Abschliessende Anweisung am Ende der Postmortem-User-Message (FA-11).
 * @param opponentSystem            Rollen-/Aufgabenbeschreibung für die KI-Vorbereitung (TEN-51).
 * @param opponentUserInstruction   Abschliessende Anweisung am Ende der Vorbereitungs-User-Message.
 */
@ConfigurationProperties(prefix = "tsas.ai.prompt")
public record PromptProperties(

        @DefaultValue("""
                Du bist ein erfahrener Tennis-Coach.
                Analysiere die übergebenen Match-Statistiken und liefere eine strukturierte taktische Auswertung.
                Antworte ausschließlich in deutscher Sprache.
                Halte dich strikt an das vorgegebene JSON-Schema. Liefere 3 bis 5 priorisierte Empfehlungen.
                """)
        String system,

        @DefaultValue("Liefere als Auswertung die vier Textfelder und 3-5 priorisierte Empfehlungen.")
        String userInstruction,

        @DefaultValue("""
                Du bist ein erfahrener Tennis-Coach, der eine Spielerin oder einen Spieler auf das nächste Match
                gegen einen bestimmten Gegner vorbereitet. Du erhältst die Head-to-Head-Statistik (kumuliert über
                alle bisherigen Begegnungen). Antworte ausschließlich in deutscher Sprache und liefere eine
                vorausschauende taktische Vorbereitung, keine Auswertung eines konkreten beendeten Matches.
                Halte dich strikt an das vorgegebene JSON-Schema. Liefere 3 bis 5 priorisierte Empfehlungen.
                """)
        String opponentSystem,

        @DefaultValue("""
                Liefere die folgenden Textfelder: opponentProfile (kompaktes Spieler-Profil des Gegners),
                tacticalObservations (Muster und Tendenzen aus dem Head-to-Head), serveStrategy (was der eigene
                Spieler im Aufschlagspiel gegen diesen Gegner umsetzen sollte), returnStrategy (was beim Return
                wichtig ist) sowie 3-5 priorisierte Empfehlungen (Recommendation mit priority, title, detail).
                """)
        String opponentUserInstruction
) {
}
