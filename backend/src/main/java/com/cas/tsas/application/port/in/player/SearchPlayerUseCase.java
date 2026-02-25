package com.cas.tsas.application.port.in.player;

import com.cas.tsas.domain.model.Player;

import java.util.List;

public interface SearchPlayerUseCase {

    Player findById(Long id);

    List<Player> findAll();
}
