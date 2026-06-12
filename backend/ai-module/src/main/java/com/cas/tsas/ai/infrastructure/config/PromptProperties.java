package com.cas.tsas.ai.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Externalisierte LLM-Prompt-Bausteine. Bindet {@code tsas.ai.prompt.*} aus der
 * Konfiguration; die {@link DefaultValue}-Texte dienen als sicherer Fallback und können per
 * {@code application.yml} oder Umgebungsvariable (z. B. {@code TSAS_AI_PROMPT_SYSTEM})
 * überschrieben werden — etwa zur Sprach- oder Tonalitätsanpassung ohne Neu-Build.
 *
 * @param system          Rollen-/Aufgabenbeschreibung für die System-Message.
 * @param userInstruction Abschliessende Anweisung am Ende der User-Message.
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
        String userInstruction
) {
}
