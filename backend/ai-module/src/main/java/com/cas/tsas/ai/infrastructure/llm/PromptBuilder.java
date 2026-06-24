package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.ai.infrastructure.config.PromptProperties;
import com.cas.tsas.statistics.domain.model.HeadToHeadPlayerStats;
import com.cas.tsas.statistics.domain.model.HeadToHeadStatistics;
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

    /** System prompt für die KI-Vorbereitung gegen einen Gegner (TEN-51). */
    public String opponentPreparationSystemPrompt() {
        return prompts.opponentSystem();
    }

    /**
     * Renders the user prompt for opponent preparation: own player + opponent metadata, plus the
     * Head-to-Head-Statistik (FA-08) aufgeschlüsselt pro Spieler.
     */
    public String opponentPreparationUserPrompt(HeadToHeadStatistics h2h, MatchMetadata m) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eigener Spieler: ").append(m.player1().fullName())
                .append(" (Ranking: ").append(m.player1().ranking())
                .append(", ").append(m.player1().handedness())
                .append(", Rückhand: ").append(m.player1().backhandType()).append(")\n");
        sb.append("Gegner: ").append(m.player2().fullName())
                .append(" (Ranking: ").append(m.player2().ranking())
                .append(", ").append(m.player2().handedness())
                .append(", Rückhand: ").append(m.player2().backhandType()).append(")\n");
        sb.append("Gemeinsame, abgeschlossene Matches: ").append(h2h.matchesPlayed()).append("\n\n");

        appendH2hPlayer(sb, "Eigener Spieler (kumuliert)", h2h.player1());
        sb.append("\n");
        appendH2hPlayer(sb, "Gegner (kumuliert)", h2h.player2());

        sb.append("\n").append(prompts.opponentUserInstruction());
        return sb.toString();
    }

    /** Block per Spieler mit den Head-to-Head-Kennzahlen aus FA-08 (kumuliert). */
    private void appendH2hPlayer(StringBuilder sb, String label, HeadToHeadPlayerStats p) {
        sb.append(label).append(":\n");
        sb.append("  Matches: ").append(p.matchesWon()).append(" gewonnen / ").append(p.matchesLost()).append(" verloren\n");
        sb.append("  Sätze: ").append(p.setsWon()).append(" : ").append(p.setsLost()).append("\n");
        sb.append("  Aufschlag — Aces: ").append(p.aces())
                .append(", Doppelfehler: ").append(p.doubleFaults()).append("\n");
        sb.append(String.format(Locale.GERMAN, "  1. Aufschlag rein: %.0f %%, gewonnen: %.0f %%%n",
                100 * p.firstServePercentage(), 100 * p.firstServeWonPercentage()));
        sb.append(String.format(Locale.GERMAN, "  2. Aufschlag gewonnen: %.0f %%%n",
                100 * p.secondServeWonPercentage()));
        sb.append(String.format(Locale.GERMAN, "  Return-Punkte gewonnen (1./2.): %.0f %% / %.0f %%%n",
                100 * p.returnPointsWonFirstPercentage(), 100 * p.returnPointsWonSecondPercentage()));
        sb.append("  Breakpoints: ").append(p.breakPointsWon())
                .append(" von ").append(p.breakPointsPlayed())
                .append(String.format(Locale.GERMAN, " (%.0f %%)%n", 100 * p.breakPointsWonPercentage()));
        sb.append(String.format(Locale.GERMAN, "  Return Games gewonnen: %.0f %%%n",
                100 * p.returnGamesWonPercentage()));
        sb.append("  Winners: ").append(p.winners())
                .append(String.format(Locale.GERMAN, " (%.0f %%)", 100 * p.winnersPercentage()))
                .append(", Unforced Errors: ").append(p.unforcedErrors())
                .append(String.format(Locale.GERMAN, " (%.0f %%)%n", 100 * p.unforcedErrorPercentage()));
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
