/**
 * Statistics module — computes and serves match statistics (winners %, unforced errors %,
 * 1st/2nd serve %, aces, double faults, break points, stroke/direction distributions).
 *
 * <p>Aggregated statistics are computed on-the-fly from the recorded points
 * ({@code MatchStatisticsService} + {@code PointAttribution}) and delivered both to the REST
 * layer and to the {@code ai-module} as input. Structured along Clean Architecture layers
 * (infrastructure → application → domain).
 *
 * <p>Module dependencies: {@code common-module} and {@code match-module}; match points are
 * read through match's application-layer port ({@code LoadPointsByMatchPort}), while match
 * domain value types ({@code Point}, {@code PointType}, …) are reused directly as a
 * shared read model (see ADR-13).
 */
package com.cas.tsas.statistics;
