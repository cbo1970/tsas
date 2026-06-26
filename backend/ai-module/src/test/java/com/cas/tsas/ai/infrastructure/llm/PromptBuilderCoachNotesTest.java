package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import com.cas.tsas.statistics.domain.model.DirectionDistribution;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import com.cas.tsas.statistics.domain.model.StrokeDistribution;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderCoachNotesTest {

    private final PromptBuilder builder = new PromptBuilder(
            new PromptProperties("sys", "userInstruction", "oppSys", "oppUserInstruction"));

    private MatchMetadata metaWithNotes(String n1, String n2) {
        return new MatchMetadata(
                new MatchMetadata.PlayerInfo("A", "R1", "RIGHT", "TWO_HANDED"),
                new MatchMetadata.PlayerInfo("B", "R2", "LEFT", "ONE_HANDED"),
                2, false, false, n1, n2, List.of());
    }

    private MatchStatistics stats() {
        PlayerStatistics p = new PlayerStatistics(1, 10, 5, 4, 1, 2, 1, 0.6, 0.5,
                1, 3, new StrokeDistribution(Map.of()), new DirectionDistribution(Map.of()));
        return new MatchStatistics(UUID.randomUUID(), p, p, 20, 7, Instant.now());
    }

    @Test
    void includes_both_notes_when_present() {
        String prompt = builder.userPrompt(stats(), metaWithNotes("RH longline schwach", "VH inside-out stark"));
        assertThat(prompt).contains("Coach-Beobachtungen");
        assertThat(prompt).contains("eigener Spieler): RH longline schwach");
        assertThat(prompt).contains("Gegner): VH inside-out stark");
    }

    @Test
    void omits_block_entirely_when_no_notes() {
        String prompt = builder.userPrompt(stats(), metaWithNotes(null, "   "));
        // " " is blank -> no player2 line; player1 null -> no block at all
        assertThat(prompt).doesNotContain("Coach-Beobachtungen");
    }
}
