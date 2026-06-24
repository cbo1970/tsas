package com.cas.tsas.app.me;

import com.cas.tsas.ai.infrastructure.persistence.entity.MatchAnalysisJpaEntity;
import com.cas.tsas.ai.infrastructure.persistence.repository.MatchAnalysisJpaRepository;
import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.MatchScoreJpaRepository;
import com.cas.tsas.match.infrastructure.persistence.repository.PointJpaRepository;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.repository.PlayerJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DSGVO Art. 17 (Right to Erasure) and Art. 20 (Right to Data Portability) workflow.
 * Lives in the {@code app}-Modul because it cross-cuts player, match and ai aggregates —
 * the composition root is the right place for orchestration across module boundaries.
 *
 * <p>Both operations resolve the acting user via {@link CurrentUserProvider}; there is no
 * cross-tenant entry point, so the controller does not accept a userId parameter.
 */
@Service
public class MeService {

    private static final Logger LOG = LoggerFactory.getLogger(MeService.class);

    private final CurrentUserProvider currentUserProvider;
    private final PlayerJpaRepository playerRepo;
    private final MatchJpaRepository matchRepo;
    private final PointJpaRepository pointRepo;
    private final MatchScoreJpaRepository matchScoreRepo;
    private final MatchAnalysisJpaRepository matchAnalysisRepo;

    public MeService(CurrentUserProvider currentUserProvider,
                     PlayerJpaRepository playerRepo,
                     MatchJpaRepository matchRepo,
                     PointJpaRepository pointRepo,
                     MatchScoreJpaRepository matchScoreRepo,
                     MatchAnalysisJpaRepository matchAnalysisRepo) {
        this.currentUserProvider = currentUserProvider;
        this.playerRepo = playerRepo;
        this.matchRepo = matchRepo;
        this.pointRepo = pointRepo;
        this.matchScoreRepo = matchScoreRepo;
        this.matchAnalysisRepo = matchAnalysisRepo;
    }

    /** DSGVO Art. 20: full snapshot of everything the user has produced. */
    @Transactional(readOnly = true)
    public UserDataExport exportCurrentUserData() {
        UUID userId = currentUserProvider.get().id();
        List<PlayerJpaEntity> players = playerRepo.findAllByOwnerId(userId);
        List<MatchJpaEntity> matches = matchRepo.findAllByOwnerId(userId);
        Set<UUID> matchIds = matches.stream().map(MatchJpaEntity::getId).collect(Collectors.toSet());
        List<PointJpaEntity> points = matchIds.isEmpty()
                ? List.of()
                : matches.stream()
                        .flatMap(m -> pointRepo.findAllByMatchIdOrderBySetNumberAscGameNumberAscPointNumberAsc(m.getId()).stream())
                        .toList();
        List<MatchScoreJpaEntity> scores = matchIds.isEmpty()
                ? List.of()
                : matches.stream()
                        .flatMap(m -> matchScoreRepo.findByMatchId(m.getId()).stream())
                        .toList();
        List<MatchAnalysisJpaEntity> analyses = matchIds.isEmpty()
                ? List.of()
                : matchAnalysisRepo.findAllByMatchIdIn(matchIds);
        return new UserDataExport(
                UserDataExport.Header.now(userId),
                players, matches, points, scores, analyses);
    }

    /**
     * DSGVO Art. 17: deletes every aggregate the user owns. FK order matters because not all
     * child tables cascade (only match_analysis has ON DELETE CASCADE; points and match_scores
     * are deleted explicitly via @Modifying queries).
     */
    @Transactional
    public DeletionSummary deleteCurrentUserData() {
        UUID userId = currentUserProvider.get().id();
        List<MatchJpaEntity> matches = matchRepo.findAllByOwnerId(userId);
        Set<UUID> matchIds = matches.stream().map(MatchJpaEntity::getId).collect(Collectors.toSet());

        int pointsDeleted = matchIds.isEmpty() ? 0 : pointRepo.deleteAllByMatchIdIn(matchIds);
        int scoresDeleted = matchIds.isEmpty() ? 0 : matchScoreRepo.deleteAllByMatchIdIn(matchIds);
        long matchesDeleted = matchRepo.deleteAllByOwnerId(userId);
        long playersDeleted = playerRepo.deleteAllByOwnerId(userId);
        // match_analysis cascades via FK ON DELETE CASCADE when matches are removed.

        DeletionSummary summary = new DeletionSummary(
                userId, (int) playersDeleted, (int) matchesDeleted, pointsDeleted, scoresDeleted);
        LOG.info("DSGVO Art. 17 delete: user={} players={} matches={} points={} scores={}",
                userId, summary.players(), summary.matches(), summary.points(), summary.scores());
        return summary;
    }

    /** Counts emitted by {@link #deleteCurrentUserData()} and surfaced to the API caller for audit purposes. */
    public record DeletionSummary(UUID userId, int players, int matches, int points, int scores) {
    }
}
