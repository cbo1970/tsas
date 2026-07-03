package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.application.port.out.MatchHistoryRow;
import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface MatchJpaRepository extends JpaRepository<MatchJpaEntity, UUID> {

    Optional<MatchJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<MatchJpaEntity> findAllByOwnerId(UUID ownerId);

    boolean existsByPlayer1IdOrPlayer2Id(UUID player1Id, UUID player2Id);

    @Query("SELECT m FROM MatchJpaEntity m WHERE m.status = :status AND (m.player1Id IN :ids OR m.player2Id IN :ids)")
    List<MatchJpaEntity> findByStatusAndPlayerIdIn(@Param("status") MatchStatus status, @Param("ids") Set<UUID> ids);

    @Query("SELECT COUNT(m) > 0 FROM MatchJpaEntity m WHERE m.status = :status AND (m.player1Id = :playerId OR m.player2Id = :playerId)")
    boolean existsByStatusAndPlayerId(@Param("status") MatchStatus status, @Param("playerId") UUID playerId);

    @Query("SELECT m FROM MatchJpaEntity m WHERE (m.player1Id = :playerA AND m.player2Id = :playerB) "
            + "OR (m.player1Id = :playerB AND m.player2Id = :playerA)")
    List<MatchJpaEntity> findMatchesBetween(@Param("playerA") UUID playerA, @Param("playerB") UUID playerB);

    /** Bulk-delete used by the DSGVO Art. 17 endpoint (TEN-66). Caller deletes child rows first. */
    long deleteAllByOwnerId(UUID ownerId);

    @Query("""
            SELECT new com.cas.tsas.match.application.port.out.MatchHistoryRow(
                m.id, m.player1Id, m.player2Id, ms.setsPlayer1, ms.setsPlayer2, ms.winner, m.updatedAt)
            FROM MatchJpaEntity m, MatchScoreJpaEntity ms
            WHERE ms.matchId = m.id
              AND (m.player1Id = :playerId OR m.player2Id = :playerId)
              AND m.status = com.cas.tsas.match.domain.model.MatchStatus.COMPLETED
              AND m.ownerId = :ownerId
            ORDER BY m.updatedAt DESC
            """)
    List<MatchHistoryRow> findCompletedHistoryByPlayer(@Param("playerId") UUID playerId,
                                                       @Param("ownerId") UUID ownerId);
}
