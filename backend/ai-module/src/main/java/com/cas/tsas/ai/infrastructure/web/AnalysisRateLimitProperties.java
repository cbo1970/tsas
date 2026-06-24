package com.cas.tsas.ai.infrastructure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-user rate limit for the AI match-analysis endpoint. Two refill bands run
 * simultaneously — a long-window daily cap to bound cost and a short-window
 * minute cap to bound burst. A call needs a token from both buckets.
 */
@ConfigurationProperties(prefix = "tsas.ai.rate-limit")
public record AnalysisRateLimitProperties(int perDay, int perMinute) {

    public AnalysisRateLimitProperties {
        if (perDay <= 0) {
            throw new IllegalArgumentException("tsas.ai.rate-limit.per-day must be > 0");
        }
        if (perMinute <= 0) {
            throw new IllegalArgumentException("tsas.ai.rate-limit.per-minute must be > 0");
        }
        if (perMinute > perDay) {
            throw new IllegalArgumentException("per-minute must not exceed per-day");
        }
    }
}
