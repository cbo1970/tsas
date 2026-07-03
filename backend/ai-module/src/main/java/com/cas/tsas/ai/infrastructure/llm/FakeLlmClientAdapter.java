package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.dto.OpponentPreparationResult;
import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import com.cas.tsas.statistics.domain.model.MatchStatistics;

import java.util.List;

/**
 * Deterministic {@link LlmClientPort} fallback that returns canned analysis text without calling
 * any external LLM. Used as the default when no other {@link LlmClientPort} bean is present.
 */
public class FakeLlmClientAdapter implements LlmClientPort {

    public static final String MODEL_NAME = "fake-llm";

    @Override
    public MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta, String language) {
        return new MatchAnalysisResult(
                "Schlüsselmomente (Fake): " + stats.totalPoints() + " Punkte gespielt",
                "Stärken eigen (Fake)",
                "Schwächen eigen (Fake)",
                "Stärken Gegner (Fake)",
                "Schwächen Gegner (Fake)",
                List.of(
                        new Recommendation(1, "Mehr Aufschlag-Variation", "1./2. Aufschlag mischen."),
                        new Recommendation(2, "Rückhand Cross spielen", "Gegner ist auf der RH schwächer.")
                )
        );
    }

    @Override
    public OpponentPreparationResult generateOpponentPreparation(HeadToHeadStatistics h2h, MatchMetadata meta, String language) {
        String opponentName = meta.player2().fullName();
        return new OpponentPreparationResult(
                "Profil " + opponentName + " (Fake): " + h2h.matchesPlayed() + " gemeinsame Matches.",
                "Taktische Beobachtungen (Fake): Gegner stark im Aufschlag, Rückhand-Cross verbesserbar.",
                "Aufschlag-Strategie (Fake): Variieren zwischen T und Body, 2. Aufschlag mit Kick.",
                "Return-Strategie (Fake): Position weiter hinten, aggressiv auf 2. Aufschlag.",
                List.of(
                        new Recommendation(1, "Rückhand-Cross attackieren", "Gegner zeigt höhere Fehlerquote auf RH-Cross."),
                        new Recommendation(2, "Lange Ballwechsel suchen", "Gegner verliert Konzentration nach 8+ Schlägen.")
                )
        );
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }
}
