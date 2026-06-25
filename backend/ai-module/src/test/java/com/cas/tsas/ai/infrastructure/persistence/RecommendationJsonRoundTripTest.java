package com.cas.tsas.ai.infrastructure.persistence;

import com.cas.tsas.ai.domain.model.Recommendation;
import com.cas.tsas.ai.domain.model.RecommendationStatus;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schützt die JSON-Persistenz der Empfehlungen (Spalte match_analysis.recommendations):
 * neue Review-Felder serialisieren mit, und Alt-JSON ohne die Felder bleibt lesbar.
 */
class RecommendationJsonRoundTripTest {

    private static final TypeReference<List<Recommendation>> LIST = new TypeReference<>() {};
    private final ObjectMapper mapper = JsonMapper.builder().build();

    @Test
    void roundTrip_preservesReviewState() {
        Recommendation r = new Recommendation(1, "Serve wide", "Detail")
                .withReview(RecommendationStatus.REJECTED, "zu riskant",
                        Instant.parse("2026-06-25T10:00:00Z"));

        String json = mapper.writeValueAsString(List.of(r));
        List<Recommendation> back = mapper.readValue(json, LIST);

        assertThat(back).hasSize(1);
        assertThat(back.get(0)).isEqualTo(r);
    }

    @Test
    void legacyJsonWithoutReviewFields_deserializesToOpen() {
        String legacy = "[{\"priority\":1,\"title\":\"t\",\"detail\":\"d\"}]";

        List<Recommendation> back = mapper.readValue(legacy, LIST);

        assertThat(back).hasSize(1);
        assertThat(back.get(0).status()).isEqualTo(RecommendationStatus.OPEN);
        assertThat(back.get(0).reviewNote()).isNull();
        assertThat(back.get(0).reviewedAt()).isNull();
    }
}
