package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MatchScoreJpaRepository extends JpaRepository<MatchScoreJpaEntity, UUID> {

    Optional<MatchScoreJpaEntity> findByMatchId(UUID matchId);

    /** Bulk-delete child rows for the DSGVO Art. 17 endpoint (TEN-66) — fires before matches are removed. */
    @Modifying
    @Query("DELETE FROM MatchScoreJpaEntity s WHERE s.matchId IN :matchIds")
    int deleteAllByMatchIdIn(@Param("matchIds") Collection<UUID> matchIds);
}
