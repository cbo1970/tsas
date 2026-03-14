package com.cas.tsas.application.port.in.player;

import com.cas.tsas.domain.model.Player;

import java.util.List;
import java.util.UUID;

public interface SearchPlayerUseCase {

    Player findById(UUID id);

    List<Player> findAll();
}
