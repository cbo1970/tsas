package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Builds the system and user prompts sent to the LLM from match statistics and metadata.
 *
 * <p>The fixed prompt texts (system instruction, user instruction) are externalised via
 * {@link PromptProperties}; this class only assembles the variable, data-driven part.
 */
@Component
public class PromptBuilder {

    private final PromptProperties prompts;

    public PromptBuilder(PromptProperties prompts) {
        this.prompts = prompts;
    }

    /** Returns the externalised system prompt that frames the LLM's role. */
    public String systemPrompt() {
        return prompts.system();
    }

    /**
     * Renders the user prompt: player metadata and the match format header, followed by the
     * per-player statistics block, closed by the externalised user instruction.
     */
    public String userPrompt(MatchStatistics s, MatchMetadata m) {
        StringBuilder sb = new StringBuilder();
        sb.append("Spieler 1: ").append(m.player1().fullName())
                .append(" (Ranking: ").append(m.player1().ranking())
                .append(", ").append(m.player1().handedness())
                .append(", Rückhand: ").append(m.player1().backhandType()).append(")\n");
        sb.append("Spieler 2: ").append(m.player2().fullName())
                .append(" (Ranking: ").append(m.player2().ranking())
                .append(", ").append(m.player2().handedness())
                .append(", Rückhand: ").append(m.player2().backhandType()).append(")\n");
        sb.append("Match-Format: Best-of-").append(2 * m.setsToWin() - 1)
                .append(", Match-Tiebreak: ").append(m.matchTiebreak())
                .append(", Short Set: ").append(m.shortSet()).append("\n\n");

        sb.append("Gesamtpunkte: ").append(s.totalPoints())
                .append(" / Breakpoints gesamt: ").append(s.breakPointsTotal()).append("\n\n");

        appendPlayer(sb, "Spieler 1", s.player1());
        sb.append("\n");
        appendPlayer(sb, "Spieler 2", s.player2());

        sb.append("\n").append(prompts.userInstruction());
        return sb.toString();
    }

    /** Appends one player's statistics block (counts, serve percentages, distributions) under
     *  the given label, formatting the percentages with German locale. */
    private void appendPlayer(StringBuilder sb, String label, PlayerStatistics p) {
        sb.append(label).append(":\n");
        sb.append("  Punkte gewonnen: ").append(p.pointsWon()).append("\n");
        sb.append("  Winner: ").append(p.winners()).append("\n");
        sb.append("  Unforced Errors: ").append(p.unforcedErrors()).append("\n");
        sb.append("  Forced Errors: ").append(p.forcedErrors()).append("\n");
        sb.append("  Aces: ").append(p.aces()).append("\n");
        sb.append("  Doppelfehler: ").append(p.doubleFaults()).append("\n");
        sb.append(String.format(Locale.GERMAN, "  1. Aufschlag rein: %.0f %%%n", 100 * p.firstServePercentage()));
        sb.append(String.format(Locale.GERMAN, "  2. Aufschlag rein: %.0f %%%n", 100 * p.secondServePercentage()));
        sb.append("  Breakpoints gewonnen / abgewehrt: ")
                .append(p.breakPointsWon()).append(" / ").append(p.breakPointsFaced()).append("\n");
        sb.append("  Schlagverteilung: ").append(p.strokeDistribution().counts()).append("\n");
        sb.append("  Richtungsverteilung: ").append(p.directionDistribution().counts()).append("\n");
    }
}
