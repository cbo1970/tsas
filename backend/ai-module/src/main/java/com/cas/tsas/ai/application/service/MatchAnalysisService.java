package com.cas.tsas.ai.application.service;

import com.cas.tsas.ai.application.dto.MatchAnalysisResult;
import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.port.in.GenerateMatchAnalysisUseCase;
import com.cas.tsas.ai.application.port.in.GetMatchAnalysisUseCase;
import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.application.port.out.LoadMatchAnalysisPort;
import com.cas.tsas.ai.application.port.out.SaveMatchAnalysisPort;
import com.cas.tsas.ai.domain.exception.AnalysisGenerationException;
import com.cas.tsas.ai.domain.exception.InsufficientMatchDataException;
import com.cas.tsas.ai.domain.exception.MatchNotCompletedException;
import com.cas.tsas.ai.domain.model.AnalysisStatus;
import com.cas.tsas.ai.domain.model.MatchAnalysis;
import com.cas.tsas.match.application.port.in.GetMatchUseCase;
import com.cas.tsas.match.domain.model.Match;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.statistics.application.port.in.ComputeMatchStatisticsUseCase;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates AI-based match analysis: loads the match, its statistics and the involved players,
 * delegates to an {@link LlmClientPort} to produce the analysis and persists the result.
 */
@Service
public class MatchAnalysisService implements GenerateMatchAnalysisUseCase, GetMatchAnalysisUseCase {

    private final GetMatchUseCase getMatchUseCase;
    private final LoadPlayerPort loadPlayerPort;
    private final ComputeMatchStatisticsUseCase statisticsUseCase;
    private final LlmClientPort llmClient;
    private final SaveMatchAnalysisPort savePort;
    private final LoadMatchAnalysisPort loadPort;
    private final int minPointsForAnalysis;

    public MatchAnalysisService(GetMatchUseCase getMatchUseCase,
                                LoadPlayerPort loadPlayerPort,
                                ComputeMatchStatisticsUseCase statisticsUseCase,
                                LlmClientPort llmClient,
                                SaveMatchAnalysisPort savePort,
                                LoadMatchAnalysisPort loadPort,
                                @Value("${tsas.ai.min-points-for-analysis:10}") int minPointsForAnalysis) {
        this.getMatchUseCase = getMatchUseCase;
        this.loadPlayerPort = loadPlayerPort;
        this.statisticsUseCase = statisticsUseCase;
        this.llmClient = llmClient;
        this.savePort = savePort;
        this.loadPort = loadPort;
        this.minPointsForAnalysis = minPointsForAnalysis;
    }

    /**
     * Generates and stores an analysis for the given match.
     *
     * <p>The match must be {@link MatchStatus#COMPLETED} and have at least the configured minimum
     * number of points; otherwise a {@link MatchNotCompletedException} respectively
     * {@link InsufficientMatchDataException} is thrown. On a successful LLM call the COMPLETED
     * result is persisted and returned. If the LLM call fails, a FAILED record is persisted and an
     * {@link AnalysisGenerationException} is thrown.
     */
    @Override
    public MatchAnalysis generate(UUID matchId) {
        Match match = getMatchUseCase.findById(matchId);
        if (match.getStatus() != MatchStatus.COMPLETED) {
            throw new MatchNotCompletedException(matchId);
        }

        MatchStatistics stats = statisticsUseCase.compute(matchId);
        if (stats.totalPoints() < minPointsForAnalysis) {
            throw new InsufficientMatchDataException(
                    "Match must have at least " + minPointsForAnalysis + " points (found " +
                            stats.totalPoints() + ")");
        }

        MatchMetadata meta = buildMetadata(match);

        try {
            MatchAnalysisResult result = llmClient.generateAnalysis(stats, meta);
            return savePort.save(buildSuccess(matchId, result));
        } catch (RuntimeException ex) {
            savePort.save(buildFailure(matchId, ex));
            throw new AnalysisGenerationException("LLM call failed for match " + matchId, ex);
        }
    }

    @Override
    public Optional<MatchAnalysis> findByMatchId(UUID matchId) {
        return loadPort.loadByMatchId(matchId);
    }

    /** Maps a successful LLM result into a COMPLETED {@link MatchAnalysis}. */
    private MatchAnalysis buildSuccess(UUID matchId, MatchAnalysisResult r) {
        MatchAnalysis a = new MatchAnalysis();
        a.setMatchId(matchId);
        a.setStatus(AnalysisStatus.COMPLETED);
        a.setKeyMoments(r.keyMoments());
        a.setOwnStrengths(r.ownStrengths());
        a.setOwnWeaknesses(r.ownWeaknesses());
        a.setOpponentStrengths(r.opponentStrengths());
        a.setOpponentWeaknesses(r.opponentWeaknesses());
        a.setRecommendations(r.recommendations());
        a.setModelUsed(llmClient.modelName());
        a.setGeneratedAt(Instant.now());
        return a;
    }

    /** Builds a FAILED {@link MatchAnalysis} capturing the failure cause for later inspection. */
    private MatchAnalysis buildFailure(UUID matchId, RuntimeException ex) {
        MatchAnalysis a = new MatchAnalysis();
        a.setMatchId(matchId);
        a.setStatus(AnalysisStatus.FAILED);
        a.setModelUsed(llmClient.modelName());
        a.setGeneratedAt(Instant.now());
        a.setErrorMessage(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return a;
    }

    /** Resolves both players and assembles the {@link MatchMetadata} passed to the LLM prompt. */
    private MatchMetadata buildMetadata(Match match) {
        Player p1 = loadPlayerPort.loadPlayer(match.getPlayer1Id())
                .orElseThrow(() -> new PlayerNotFoundException(match.getPlayer1Id()));
        Player p2 = loadPlayerPort.loadPlayer(match.getPlayer2Id())
                .orElseThrow(() -> new PlayerNotFoundException(match.getPlayer2Id()));
        return new MatchMetadata(
                toInfo(p1), toInfo(p2),
                match.getSetsToWin(), match.isMatchTiebreak(), match.isShortSet());
    }

    private MatchMetadata.PlayerInfo toInfo(Player p) {
        return new MatchMetadata.PlayerInfo(
                p.getFirstName() + " " + p.getLastName(),
                p.getRanking(),
                p.getHandedness() == null ? null : p.getHandedness().name(),
                p.getBackhandType() == null ? null : p.getBackhandType().name());
    }
}
