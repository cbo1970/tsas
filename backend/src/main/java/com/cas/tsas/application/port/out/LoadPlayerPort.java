package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.Player;

import java.util.List;
import java.util.Optional;

public interface LoadPlayerPort {

    Optional<Player> loadPlayer(Long id);

    List<Player> loadAllPlayers();
}
