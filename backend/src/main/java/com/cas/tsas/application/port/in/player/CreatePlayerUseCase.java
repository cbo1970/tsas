package com.cas.tsas.application.port.in.player;

import com.cas.tsas.domain.model.Player;

public interface CreatePlayerUseCase {

    Player createPlayer(CreatePlayerCommand command);

    record CreatePlayerCommand(
            String name,
            String gender,
            Integer ranking,
            String handedness,
            String backhandType
    ) {}
}
