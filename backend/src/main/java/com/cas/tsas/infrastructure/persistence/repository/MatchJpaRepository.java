package com.cas.tsas.infrastructure.persistence.repository;

import com.cas.tsas.infrastructure.persistence.entity.MatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MatchJpaRepository extends JpaRepository<MatchJpaEntity, UUID> {

    boolean existsByPlayer1IdOrPlayer2Id(UUID player1Id, UUID player2Id);
}
