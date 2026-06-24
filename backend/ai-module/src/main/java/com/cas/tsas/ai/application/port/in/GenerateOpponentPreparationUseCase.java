package com.cas.tsas.ai.application.port.in;

import com.cas.tsas.ai.domain.model.OpponentPreparation;

import java.util.UUID;

/** Generiert eine KI-Vorbereitung gegen einen bestimmten Gegner (TEN-51 / Roadmap V2). */
public interface GenerateOpponentPreparationUseCase {

    /**
     * Erzeugt eine taktische Vorbereitung des eigenen Spielers gegen den Gegner auf Basis der
     * Head-to-Head-Statistik (FA-08).
     *
     * @throws com.cas.tsas.player.domain.exception.PlayerNotFoundException wenn einer der
     *     beiden Spieler nicht existiert (→ HTTP 404).
     * @throws com.cas.tsas.ai.domain.exception.InsufficientHeadToHeadDataException wenn die
     *     beiden Spieler noch kein gemeinsames abgeschlossenes Match haben (→ HTTP 422).
     * @throws com.cas.tsas.ai.domain.exception.AnalysisGenerationException wenn der LLM-Aufruf
     *     scheitert oder eine ungültige Antwort liefert (→ HTTP 502).
     */
    OpponentPreparation generate(UUID ownPlayerId, UUID opponentId);
}
