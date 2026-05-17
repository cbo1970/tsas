package com.cas.tsas.statistics.domain;

import com.cas.tsas.match.domain.model.Point;

public final class PointAttribution {

    private PointAttribution() {}

    public static int attributingPlayer(Point p) {
        return switch (p.getPointType()) {
            case WINNER -> p.getWinner();
            case ACE, DOUBLE_FAULT -> {
                if (p.getServingPlayer() == null) {
                    throw new IllegalStateException(
                            "Point " + p.getId() + " of type " + p.getPointType() +
                            " has no servingPlayer set");
                }
                yield p.getServingPlayer();
            }
            case UNFORCED_ERROR, FORCED_ERROR, NET, OUT_LONG, OUT_SIDE ->
                    p.getWinner() == 1 ? 2 : 1;
        };
    }
}
