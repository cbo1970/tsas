package com.cas.tsas.match.infrastructure.persistence.repository;

import com.cas.tsas.match.infrastructure.persistence.entity.MatchPlayerNoteJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchPlayerNoteJpaRepository extends JpaRepository<MatchPlayerNoteJpaEntity, UUID> {

    Optional<MatchPlayerNoteJpaEntity> findByMatchIdAndPlayerId(UUID matchId, UUID playerId);

    List<MatchPlayerNoteJpaEntity> findByMatchId(UUID matchId);

    List<MatchPlayerNoteJpaEntity> findByPlayerIdOrderByUpdatedAtDesc(UUID playerId, Pageable pageable);

    void deleteByMatchIdAndPlayerId(UUID matchId, UUID playerId);
}
