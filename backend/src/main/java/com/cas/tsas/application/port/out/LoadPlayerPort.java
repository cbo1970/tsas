package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPlayerPort {

    Optional<Player> loadPlayer(UUID id);

    List<Player> loadAllPlayers();
}
