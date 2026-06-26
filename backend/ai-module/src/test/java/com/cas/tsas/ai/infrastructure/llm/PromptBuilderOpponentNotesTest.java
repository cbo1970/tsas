package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderOpponentNotesTest {

    private final PromptBuilder builder = new PromptBuilder(
            new PromptProperties("sys", "userInstruction", "oppSys", "oppUserInstruction"));

    private MatchMetadata meta(List<String> opponentNotes) {
        return new MatchMetadata(
                new MatchMetadata.PlayerInfo("Own", "R1", "RIGHT", "TWO_HANDED"),
                new MatchMetadata.PlayerInfo("Opp", "R2", "LEFT", "ONE_HANDED"),
                2, false, false, null, null, opponentNotes);
    }

    private HeadToHeadStatistics h2h() {
        // HeadToHeadPlayerStats(UUID playerId,
        //   double firstServePercentage, double firstServeWonPercentage, double secondServeWonPercentage,
        //   int aces, int doubleFaults,
        //   double returnPointsWonFirstPercentage, double returnPointsWonSecondPercentage,
        //   int breakPointsWon, int breakPointsPlayed, double breakPointsWonPercentage,
        //   double returnGamesWonPercentage,
        //   int winners, int unforcedErrors, double winnersPercentage, double unforcedErrorPercentage,
        //   int matchesWon, int matchesLost, int setsWon, int setsLost)
        HeadToHeadPlayerStats s = new HeadToHeadPlayerStats(
                UUID.randomUUID(),
                0.6, 0.5, 0.4,
                2, 4,
                0.5, 0.3,
                3, 5, 0.6,
                0.5,
                10, 8, 0.3, 0.2,
                1, 0, 2, 1);
        // HeadToHeadStatistics(UUID player1Id, UUID player2Id, int matchesPlayed,
        //   HeadToHeadPlayerStats player1, HeadToHeadPlayerStats player2)
        return new HeadToHeadStatistics(UUID.randomUUID(), UUID.randomUUID(), 2, s, s);
    }

    @Test
    void includes_opponent_notes_when_present() {
        String prompt = builder.opponentPreparationUserPrompt(h2h(),
                meta(List.of("RH-Slice unter Druck schwach", "2. Aufschlag chip-bar")));
        assertThat(prompt).contains("Frühere Coach-Beobachtungen zu diesem Gegner");
        assertThat(prompt).contains("- RH-Slice unter Druck schwach");
        assertThat(prompt).contains("- 2. Aufschlag chip-bar");
    }

    @Test
    void omits_block_when_no_notes() {
        String prompt = builder.opponentPreparationUserPrompt(h2h(), meta(List.of()));
        assertThat(prompt).doesNotContain("Frühere Coach-Beobachtungen");
    }
}
