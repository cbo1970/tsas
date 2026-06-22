package com.cas.tsas.player.infrastructure.persistence.repository;

import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerJpaRepository extends JpaRepository<PlayerJpaEntity, UUID> {

    Optional<PlayerJpaEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<PlayerJpaEntity> findAllByOwnerId(UUID ownerId);
}
