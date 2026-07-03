package com.cas.tsas.match.application.port.out;

import java.util.UUID;

/**
 * Output port used to determine the next point number within a game by
 * counting the points already recorded for a given set and game.
 */
public interface CountPointsInGamePort {

    int countPointsInGame(UUID matchId, int setNumber, int gameNumber);
}
