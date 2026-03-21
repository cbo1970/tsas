package com.cas.tsas.player.application.port.in;

import java.util.UUID;

public interface DeletePlayerUseCase {

    boolean hasMatches(UUID id);

    void deletePlayer(UUID id);

    void deactivatePlayer(UUID id);
}
