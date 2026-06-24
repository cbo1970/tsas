package com.cas.tsas.ai.application.port.out;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.dto.OpponentPreparationResult;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import com.cas.tsas.statistics.domain.model.MatchStatistics;

public interface LlmClientPort {

    /** Default-Variante (Deutsch). Bleibt für Tests und Backward-Compat erhalten. */
    default MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta) {
        return generateAnalysis(stats, meta, "de");
    }

    /** TEN-6: gleicher Aufruf wie {@link #generateAnalysis(MatchStatistics, MatchMetadata)}, aber
     *  mit explizitem Sprachcode (de|en|it|fr) — der LLM antwortet in dieser Sprache. */
    MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta, String language);

    /**
     * TEN-51: generiert eine taktische Vorbereitung gegen den Gegner. {@code meta.player1} ist
     * der eigene Spieler, {@code meta.player2} der Gegner. {@code matchTiebreak}/{@code shortSet}
     * dürfen für diesen Use-Case mit beliebigen Werten gesetzt sein — sie werden im Prompt
     * nicht verwendet, da noch kein konkretes Match existiert.
     */
    default OpponentPreparationResult generateOpponentPreparation(HeadToHeadStatistics h2h, MatchMetadata meta) {
        return generateOpponentPreparation(h2h, meta, "de");
    }

    /** TEN-6: Sprachvariante von {@link #generateOpponentPreparation(HeadToHeadStatistics, MatchMetadata)}. */
    OpponentPreparationResult generateOpponentPreparation(HeadToHeadStatistics h2h, MatchMetadata meta, String language);

    String modelName();
}
