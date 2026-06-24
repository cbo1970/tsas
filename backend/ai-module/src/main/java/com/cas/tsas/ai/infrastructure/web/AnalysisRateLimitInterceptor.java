package com.cas.tsas.ai.infrastructure.web;

import com.cas.tsas.auth.application.port.in.CurrentUserProvider;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.UUID;

/**
 * Gate for {@code POST /api/matches/{matchId}/analysis}. GET requests pass through
 * unchanged — only generation triggers an LLM call, only generation is throttled.
 * On rejection writes RFC 7807 ProblemDetail with HTTP 429, sets {@code Retry-After}
 * and {@code X-RateLimit-*} response headers, and increments a Micrometer counter
 * tagged with the outcome and user id.
 */
@Component
public class AnalysisRateLimitInterceptor implements HandlerInterceptor {

    static final String METRIC_NAME = "tsas.ai.calls.total";

    private final AnalysisRateLimiter limiter;
    private final CurrentUserProvider currentUserProvider;
    private final MeterRegistry meterRegistry;

    public AnalysisRateLimitInterceptor(AnalysisRateLimiter limiter,
                                        CurrentUserProvider currentUserProvider,
                                        MeterRegistry meterRegistry) {
        this.limiter = limiter;
        this.currentUserProvider = currentUserProvider;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        UUID userId = currentUserProvider.get().id();
        AnalysisRateLimiter.Decision decision = limiter.tryConsume(userId);

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        String outcome = decision.allowed() ? "allowed" : "rate_limited";
        meterRegistry.counter(METRIC_NAME, "outcome", outcome, "user", userId.toString()).increment();

        if (decision.allowed()) {
            return true;
        }

        long retryAfter = decision.retryAfterSeconds();
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests",\
                "status":429,"detail":"AI analysis rate limit exceeded — retry in %d seconds."}\
                """.formatted(retryAfter));
        return false;
    }
}
