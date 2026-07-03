package com.cas.tsas.statistics.domain.model;

import java.util.List;

/** Total match statistics plus a per-set breakdown (TEN-36, set ascending). */
public record MatchStatisticsBreakdown(MatchStatistics total, List<SetStatistics> sets) {}
