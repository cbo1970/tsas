package com.cas.tsas.application.port.out;

import com.cas.tsas.domain.model.Player;

public interface SavePlayerPort {

    Player savePlayer(Player player);
}
