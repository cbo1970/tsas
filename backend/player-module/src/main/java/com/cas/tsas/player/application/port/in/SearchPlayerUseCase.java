package com.cas.tsas.player.application.port.in;

import com.cas.tsas.player.domain.model.Player;

import java.util.List;
import java.util.UUID;

public interface SearchPlayerUseCase {

    Player findById(UUID id);

    List<Player> findAll();
}
