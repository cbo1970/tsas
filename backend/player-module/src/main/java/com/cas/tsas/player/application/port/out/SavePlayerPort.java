package com.cas.tsas.player.application.port.out;

import com.cas.tsas.player.domain.model.Player;

public interface SavePlayerPort {

    Player savePlayer(Player player);
}
