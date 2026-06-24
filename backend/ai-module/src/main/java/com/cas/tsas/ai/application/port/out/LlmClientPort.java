package com.cas.tsas.ai.application.port.out;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.dto.OpponentPreparationResult;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import com.cas.tsas.statistics.domain.model.MatchStatistics;

public interface LlmClientPort {

    MatchAnalysisResult generateAnalysis(MatchStatistics stats, MatchMetadata meta);

    /**
     * TEN-51: generiert eine taktische Vorbereitung gegen den Gegner. {@code meta.player1} ist
     * der eigene Spieler, {@code meta.player2} der Gegner. {@code matchTiebreak}/{@code shortSet}
     * dürfen für diesen Use-Case mit beliebigen Werten gesetzt sein — sie werden im Prompt
     * nicht verwendet, da noch kein konkretes Match existiert.
     */
    OpponentPreparationResult generateOpponentPreparation(HeadToHeadStatistics h2h, MatchMetadata meta);

    String modelName();
}
