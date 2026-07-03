package com.cas.tsas.statistics.domain.model;

import com.cas.tsas.match.domain.model.Direction;

import java.util.Map;

public record DirectionDistribution(Map<Direction, Integer> counts) {}
