package com.cas.tsas.player.application.port.out;

import com.cas.tsas.player.domain.model.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPlayerPort {

    Optional<Player> loadPlayer(UUID id);

    List<Player> loadAllPlayers();

    Optional<Player> findByIdAndOwner(UUID id, UUID ownerId);

    List<Player> findAllByOwner(UUID ownerId);
}
