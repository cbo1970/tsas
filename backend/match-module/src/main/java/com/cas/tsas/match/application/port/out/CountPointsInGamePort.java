package com.cas.tsas.match.application.port.out;

import java.util.UUID;

public interface CountPointsInGamePort {

    int countPointsInGame(UUID matchId, int setNumber, int gameNumber);
}
