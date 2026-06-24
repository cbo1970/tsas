package com.cas.tsas.ai.application.service;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.application.dto.OpponentPreparationResult;
import com.cas.tsas.ai.application.port.in.GenerateOpponentPreparationUseCase;
import com.cas.tsas.ai.application.port.out.LlmClientPort;
import com.cas.tsas.ai.application.port.out.UserLanguagePort;
import com.cas.tsas.ai.domain.exception.AnalysisGenerationException;
import com.cas.tsas.ai.domain.exception.InsufficientHeadToHeadDataException;
import com.cas.tsas.ai.domain.model.OpponentPreparation;
import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.player.application.port.out.LoadPlayerPort;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import com.cas.tsas.player.domain.model.Player;
import com.cas.tsas.statistics.application.port.in.ComputeHeadToHeadStatisticsUseCase;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Generiert eine KI-gestützte Vorbereitung gegen einen bestimmten Gegner (TEN-51 / Roadmap V2).
 *
 * <p>Im Gegensatz zur Match-Analyse (FA-11, Postmortem) wird die Vorbereitung <b>nicht
 * persistiert</b> — der Head-to-Head-Stand kann sich mit jedem neuen Match ändern, eine
 * gecachte Empfehlung wäre nach wenigen Tagen veraltet. Kostenkontrolle erfolgt über den
 * bestehenden Rate-Limiter (TEN-64, gleicher Bucket).
 */
@Service
public class OpponentPreparationService implements GenerateOpponentPreparationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(OpponentPreparationService.class);

    private final LoadPlayerPort loadPlayerPort;
    private final ComputeHeadToHeadStatisticsUseCase headToHeadUseCase;
    private final LlmClientPort llmClient;
    private final CurrentUserProvider currentUserProvider;
    private final UserLanguagePort userLanguagePort;

    public OpponentPreparationService(LoadPlayerPort loadPlayerPort,
                                      ComputeHeadToHeadStatisticsUseCase headToHeadUseCase,
                                      LlmClientPort llmClient,
                                      CurrentUserProvider currentUserProvider,
                                      UserLanguagePort userLanguagePort) {
        this.loadPlayerPort = loadPlayerPort;
        this.headToHeadUseCase = headToHeadUseCase;
        this.llmClient = llmClient;
        this.currentUserProvider = currentUserProvider;
        this.userLanguagePort = userLanguagePort;
    }

    @Override
    public OpponentPreparation generate(UUID ownPlayerId, UUID opponentId) {
        if (ownPlayerId.equals(opponentId)) {
            throw new IllegalArgumentException("ownPlayerId und opponentId müssen unterschiedlich sein");
        }

        // Owner-Scoping (TEN-51 IDOR-Schutz): beide Spieler müssen dem aktuellen Nutzer gehören.
        // findByIdAndOwner gibt empty zurück, wenn der Spieler einem anderen Owner gehört — wir
        // werfen die gleiche PlayerNotFoundException wie bei „existiert gar nicht", damit der
        // Endpoint die Existenz fremder Spieler-IDs nicht enumerable macht.
        UUID currentUserId = currentUserProvider.get().id();
        Player own = loadPlayerPort.findByIdAndOwner(ownPlayerId, currentUserId)
                .orElseThrow(() -> new PlayerNotFoundException(ownPlayerId));
        Player opponent = loadPlayerPort.findByIdAndOwner(opponentId, currentUserId)
                .orElseThrow(() -> new PlayerNotFoundException(opponentId));

        HeadToHeadStatistics h2h = headToHeadUseCase.compute(ownPlayerId, opponentId);
        if (h2h.matchesPlayed() < 1) {
            throw new InsufficientHeadToHeadDataException(
                    "Keine gemeinsamen abgeschlossenen Matches zwischen den Spielern — Vorbereitung benötigt "
                            + "mindestens 1 Head-to-Head-Match.");
        }

        MatchMetadata meta = new MatchMetadata(toInfo(own), toInfo(opponent),
                /* setsToWin */ 2, /* matchTiebreak */ false, /* shortSet */ false);

        OpponentPreparationResult result;
        try {
            result = llmClient.generateOpponentPreparation(h2h, meta, userLanguagePort.currentLanguage());
        } catch (RuntimeException ex) {
            LOG.warn("LLM call failed for opponent preparation own={} opponent={}: {}",
                    ownPlayerId, opponentId, ex.toString());
            throw new AnalysisGenerationException("LLM-Aufruf für KI-Vorbereitung fehlgeschlagen", ex);
        }

        return new OpponentPreparation(
                ownPlayerId,
                opponentId,
                h2h.matchesPlayed(),
                result.opponentProfile(),
                result.tacticalObservations(),
                result.serveStrategy(),
                result.returnStrategy(),
                result.recommendations() == null ? List.of() : result.recommendations(),
                llmClient.modelName(),
                Instant.now());
    }

    private MatchMetadata.PlayerInfo toInfo(Player p) {
        return new MatchMetadata.PlayerInfo(
                p.getFirstName() + " " + p.getLastName(),
                p.getRanking(),
                p.getHandedness() == null ? null : p.getHandedness().name(),
                p.getBackhandType() == null ? null : p.getBackhandType().name());
    }
}
