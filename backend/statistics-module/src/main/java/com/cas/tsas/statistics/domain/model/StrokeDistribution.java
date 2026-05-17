package com.cas.tsas.statistics.domain.model;

import com.cas.tsas.match.domain.model.StrokeType;

import java.util.Map;

public record StrokeDistribution(Map<StrokeType, Integer> counts) {}
