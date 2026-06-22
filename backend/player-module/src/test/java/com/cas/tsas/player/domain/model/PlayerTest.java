package com.cas.tsas.player.domain.model;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class PlayerTest {

    @Test
    void constructor_assigns_owner() {
        UUID owner = UUID.randomUUID();
        Player p = new Player(null, owner, "A", "B", null, null, null, null, null, null);
        assertThat(p.getOwnerId()).isEqualTo(owner);
    }
}
