package com.cas.tsas.common.web;

import com.cas.tsas.common.exception.ConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale, modulübergreifende Fehlerbehandlung für Querschnittsbelange. Liegt im
 * common-module (Shared Kernel) und gilt damit für alle fachlichen Module. Alle Antworten
 * folgen RFC 7807 ({@link ProblemDetail}). Fachspezifische Domain-Exceptions werden in den
 * jeweiligen Modul-Advices behandelt (z. B. {@code GlobalExceptionHandler}, {@code AiExceptionHandler}).
 */
@RestControllerAdvice
public class CommonExceptionHandler {

    /** Fachliche Zustandskonflikte → 409 Conflict. */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /** Verletzte Wertebereiche/Argumente (z. B. unbekannter Enum-Wert) → 400 Bad Request. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Bean-Validation-Fehler auf {@code @Valid @RequestBody} → 400 mit Feld-Details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(),
                    fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return problem;
    }
}
