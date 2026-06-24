package com.cas.tsas.ai.infrastructure.web;

import com.cas.tsas.ai.application.port.in.GenerateOpponentPreparationUseCase;
import com.cas.tsas.ai.infrastructure.web.dto.OpponentPreparationResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST-Endpoint für die KI-Vorbereitung gegen einen Gegner (TEN-51 / FA-20).
 * Berechnung erfolgt on-demand pro Aufruf — keine Persistenz, da sich die zugrundeliegende
 * Head-to-Head-Statistik mit jedem neuen Match ändert. Cost-Kontrolle via Rate-Limiter
 * (TEN-64, siehe {@link AnalysisRateLimitInterceptor} + {@link AiWebMvcConfig}).
 */
@RestController
@RequestMapping("/api/players/{ownPlayerId}/opponent-preparation")
public class OpponentPreparationController {

    private final GenerateOpponentPreparationUseCase generateUseCase;

    public OpponentPreparationController(GenerateOpponentPreparationUseCase generateUseCase) {
        this.generateUseCase = generateUseCase;
    }

    @PostMapping("/{opponentId}")
    public OpponentPreparationResponse generate(@PathVariable UUID ownPlayerId,
                                                @PathVariable UUID opponentId) {
        return OpponentPreparationResponse.from(generateUseCase.generate(ownPlayerId, opponentId));
    }
}
