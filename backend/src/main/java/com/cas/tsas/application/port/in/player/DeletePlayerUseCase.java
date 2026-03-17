package com.cas.tsas.application.port.in.player;

import java.util.UUID;

public interface DeletePlayerUseCase {

    boolean hasMatches(UUID id);

    void deletePlayer(UUID id);

    void deactivatePlayer(UUID id);
}
