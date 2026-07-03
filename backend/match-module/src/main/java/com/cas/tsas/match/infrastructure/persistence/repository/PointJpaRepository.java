package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PointJpaRepository extends JpaRepository<PointJpaEntity, UUID> {
    int countByMatchIdAndSetNumberAndGameNumber(UUID matchId, int setNumber, int gameNumber);

    List<PointJpaEntity> findAllByMatchIdOrderBySetNumberAscGameNumberAscPointNumberAsc(UUID matchId);

    /** Bulk-delete child rows for the DSGVO Art. 17 endpoint (TEN-66) — fires before matches are removed. */
    @Modifying
    @Query("DELETE FROM PointJpaEntity p WHERE p.matchId IN :matchIds")
    int deleteAllByMatchIdIn(@Param("matchIds") Collection<UUID> matchIds);
}
