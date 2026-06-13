package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.domain.model.MatchStatus;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MatchJpaRepository extends JpaRepository<MatchJpaEntity, UUID> {

    boolean existsByPlayer1IdOrPlayer2Id(UUID player1Id, UUID player2Id);

    @Query("SELECT m FROM MatchJpaEntity m WHERE m.status = :status AND (m.player1Id IN :ids OR m.player2Id IN :ids)")
    List<MatchJpaEntity> findByStatusAndPlayerIdIn(@Param("status") MatchStatus status, @Param("ids") Set<UUID> ids);

    @Query("SELECT COUNT(m) > 0 FROM MatchJpaEntity m WHERE m.status = :status AND (m.player1Id = :playerId OR m.player2Id = :playerId)")
    boolean existsByStatusAndPlayerId(@Param("status") MatchStatus status, @Param("playerId") UUID playerId);

    @Query("SELECT m FROM MatchJpaEntity m WHERE (m.player1Id = :playerA AND m.player2Id = :playerB) "
            + "OR (m.player1Id = :playerB AND m.player2Id = :playerA)")
    List<MatchJpaEntity> findMatchesBetween(@Param("playerA") UUID playerA, @Param("playerB") UUID playerB);
}
