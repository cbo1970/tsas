package com.cas.tsas.ai.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationTest {

    @Test
    void threeArgConstructor_defaultsToOpenWithoutReview() {
        Recommendation r = new Recommendation(1, "t", "d");
        assertThat(r.status()).isEqualTo(RecommendationStatus.OPEN);
        assertThat(r.reviewNote()).isNull();
        assertThat(r.reviewedAt()).isNull();
    }

    @Test
    void nullStatus_isNormalizedToOpen() {
        Recommendation r = new Recommendation(1, "t", "d", null, null, null);
        assertThat(r.status()).isEqualTo(RecommendationStatus.OPEN);
    }

    @Test
    void withReview_returnsUpdatedCopyKeepingContent() {
        Instant at = Instant.parse("2026-06-25T10:00:00Z");
        Recommendation r = new Recommendation(2, "Serve wide", "Detail")
                .withReview(RecommendationStatus.REJECTED, "nicht passend", at);
        assertThat(r.priority()).isEqualTo(2);
        assertThat(r.title()).isEqualTo("Serve wide");
        assertThat(r.detail()).isEqualTo("Detail");
        assertThat(r.status()).isEqualTo(RecommendationStatus.REJECTED);
        assertThat(r.reviewNote()).isEqualTo("nicht passend");
        assertThat(r.reviewedAt()).isEqualTo(at);
    }
}
