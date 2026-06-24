package com.cas.tsas.ai.infrastructure.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory per-user token bucket for the AI analysis endpoint. Single-instance
 * only — for horizontally scaled deployments, swap the {@link ConcurrentMap}
 * backend for a Bucket4j distributed store (Redis/Hazelcast).
 */
@Component
public class AnalysisRateLimiter {

    private final ConcurrentMap<UUID, Bucket> buckets = new ConcurrentHashMap<>();
    private final AnalysisRateLimitProperties props;

    public AnalysisRateLimiter(AnalysisRateLimitProperties props) {
        this.props = props;
    }

    public Decision tryConsume(UUID userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, k -> newBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return new Decision(
                probe.isConsumed(),
                Math.max(probe.getRemainingTokens(), 0),
                props.perDay(),
                probe.getNanosToWaitForRefill());
    }

    private Bucket newBucket() {
        Bandwidth daily = Bandwidth.builder()
                .capacity(props.perDay())
                .refillIntervally(props.perDay(), Duration.ofDays(1))
                .initialTokens(props.perDay())
                .build();
        Bandwidth perMinute = Bandwidth.builder()
                .capacity(props.perMinute())
                .refillIntervally(props.perMinute(), Duration.ofMinutes(1))
                .initialTokens(props.perMinute())
                .build();
        return Bucket.builder().addLimit(daily).addLimit(perMinute).build();
    }

    /** Outcome of a single consumption attempt. */
    public record Decision(boolean allowed, long remaining, long limit, long nanosToNextRefill) {

        public long retryAfterSeconds() {
            return Math.max(1L, (nanosToNextRefill + 999_999_999L) / 1_000_000_000L);
        }
    }
}
