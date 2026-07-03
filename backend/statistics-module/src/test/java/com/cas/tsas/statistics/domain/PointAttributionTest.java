package com.cas.tsas.statistics.domain;

import com.cas.tsas.match.domain.model.Point;
import com.cas.tsas.match.domain.model.PointType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PointAttributionTest {

    private Point point(int winner, PointType type, Integer server) {
        return new Point(UUID.randomUUID(), UUID.randomUUID(), 1, 1, 1,
                winner, type, null, null, server, false, null, null);
    }

    @Test
    void winnerIsAttributedToPointWinner() {
        assertThat(PointAttribution.attributingPlayer(point(1, PointType.WINNER, 2))).isEqualTo(1);
        assertThat(PointAttribution.attributingPlayer(point(2, PointType.WINNER, 1))).isEqualTo(2);
    }

    @Test
    void unforcedErrorIsAttributedToPointLoser() {
        assertThat(PointAttribution.attributingPlayer(point(1, PointType.UNFORCED_ERROR, 2))).isEqualTo(2);
        assertThat(PointAttribution.attributingPlayer(point(2, PointType.UNFORCED_ERROR, 1))).isEqualTo(1);
    }

    @Test
    void forcedErrorIsAttributedToPointLoser() {
        assertThat(PointAttribution.attributingPlayer(point(2, PointType.FORCED_ERROR, 1))).isEqualTo(1);
    }

    @Test
    void netIsAttributedToPointLoser() {
        assertThat(PointAttribution.attributingPlayer(point(2, PointType.NET, 1))).isEqualTo(1);
    }

    @Test
    void outLongIsAttributedToPointLoser() {
        assertThat(PointAttribution.attributingPlayer(point(1, PointType.OUT_LONG, 1))).isEqualTo(2);
    }

    @Test
    void outSideIsAttributedToPointLoser() {
        assertThat(PointAttribution.attributingPlayer(point(1, PointType.OUT_SIDE, 1))).isEqualTo(2);
    }

    @Test
    void aceIsAttributedToServer() {
        assertThat(PointAttribution.attributingPlayer(point(1, PointType.ACE, 1))).isEqualTo(1);
        assertThat(PointAttribution.attributingPlayer(point(1, PointType.ACE, 2))).isEqualTo(2);
    }

    @Test
    void doubleFaultIsAttributedToServer() {
        assertThat(PointAttribution.attributingPlayer(point(2, PointType.DOUBLE_FAULT, 1))).isEqualTo(1);
    }

    @Test
    void throwsWhenServeWithoutServingPlayer() {
        assertThatThrownBy(() -> PointAttribution.attributingPlayer(point(1, PointType.ACE, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("servingPlayer");
    }
}
