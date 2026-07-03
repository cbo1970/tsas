package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchPlayerNoteJpaRepository extends JpaRepository<MatchPlayerNoteJpaEntity, UUID> {

    Optional<MatchPlayerNoteJpaEntity> findByMatchIdAndPlayerId(UUID matchId, UUID playerId);

    List<MatchPlayerNoteJpaEntity> findByMatchId(UUID matchId);

    /**
     * Notes about one player, restricted to COMPLETED matches, newest first (TEN-68). Opponent
     * preparation aggregates past observations; in-progress matches are excluded so the source is
     * consistent with the Head-to-Head statistics, which also count only completed matches. The
     * note table has no JPA association to matches, hence the explicit theta-join on {@code matchId}.
     */
    @Query("""
            SELECT n FROM MatchPlayerNoteJpaEntity n, MatchJpaEntity m
            WHERE n.matchId = m.id
              AND n.playerId = :playerId
              AND m.status = com.cas.tsas.match.domain.model.MatchStatus.COMPLETED
            ORDER BY n.updatedAt DESC
            """)
    List<MatchPlayerNoteJpaEntity> findAboutPlayerInCompletedMatches(@Param("playerId") UUID playerId,
                                                                     Pageable pageable);

    void deleteByMatchIdAndPlayerId(UUID matchId, UUID playerId);
}
