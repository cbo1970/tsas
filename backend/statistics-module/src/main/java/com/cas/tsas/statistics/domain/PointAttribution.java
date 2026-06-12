package com.cas.tsas.statistics.domain;

import com.cas.tsas.match.domain.model.Point;

/**
 * Determines which of the two players a point's outcome is statistically attributed to.
 *
 * <p>Attribution is not the same as winning the point: positive actions are credited to
 * their author, while errors are charged to the player who committed them (the opponent of
 * the point winner).
 */
public final class PointAttribution {

    private PointAttribution() {}

    /**
     * Returns the player number (1 or 2) the point is attributed to:
     * <ul>
     *   <li>{@code WINNER} → the point winner (the player who hit the winning shot)</li>
     *   <li>{@code ACE}, {@code DOUBLE_FAULT} → the serving player; requires
     *       {@code servingPlayer} to be set, otherwise an {@link IllegalStateException} is thrown</li>
     *   <li>{@code UNFORCED_ERROR}, {@code FORCED_ERROR}, {@code NET}, {@code OUT_LONG},
     *       {@code OUT_SIDE} → the opponent of the point winner (the player who caused the error)</li>
     * </ul>
     *
     * @throws IllegalStateException if an ACE or DOUBLE_FAULT point has no serving player set
     */
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
