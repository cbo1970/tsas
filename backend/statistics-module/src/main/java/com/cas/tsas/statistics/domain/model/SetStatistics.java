package com.cas.tsas.statistics.domain.model;

/** Per-set slice of a match's statistics (TEN-36). */
public record SetStatistics(int setNumber, MatchStatistics stats) {}
