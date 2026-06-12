package com.cas.tsas.app.infrastructure.web;

import com.cas.tsas.match.domain.exception.MatchNotFoundException;
import com.cas.tsas.player.domain.exception.PlayerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Behandelt die modulübergreifenden „nicht gefunden"-Domain-Exceptions der fachlichen Module
 * (Player, Match) → HTTP 404. Querschnittliche Fälle (Konflikte, Validierung, ungültige
 * Argumente) übernimmt der {@code CommonExceptionHandler} im common-module; AI-spezifische
 * Fälle der {@code AiExceptionHandler}. Alle Antworten folgen RFC 7807 ({@link ProblemDetail}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PlayerNotFoundException.class)
    public ProblemDetail handlePlayerNotFound(PlayerNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MatchNotFoundException.class)
    public ProblemDetail handleMatchNotFound(MatchNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
