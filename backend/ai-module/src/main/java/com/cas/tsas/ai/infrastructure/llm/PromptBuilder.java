package com.cas.tsas.ai.infrastructure.llm;

import com.cas.tsas.ai.application.dto.MatchMetadata;
import com.cas.tsas.statistics.domain.model.MatchStatistics;
import com.cas.tsas.statistics.domain.model.PlayerStatistics;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PromptBuilder {

    public String systemPrompt() {
        return """
                Du bist ein erfahrener Tennis-Coach.
                Analysiere die übergebenen Match-Statistiken und liefere eine strukturierte taktische Auswertung.
                Antworte ausschließlich in deutscher Sprache.
                Halte dich strikt an das vorgegebene JSON-Schema. Liefere 3 bis 5 priorisierte Empfehlungen.
                """;
    }

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

        sb.append("\nLiefere als Auswertung die vier Textfelder und 3-5 priorisierte Empfehlungen.");
        return sb.toString();
    }

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
