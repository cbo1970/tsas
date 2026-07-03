package com.cas.tsas.common.web;

import com.cas.tsas.common.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Zentrale, modulübergreifende Fehlerbehandlung für Querschnittsbelange (TEN-61 / STRIDE I5+I6).
 * Liegt im common-module (Shared Kernel) und gilt damit für alle fachlichen Module. Alle
 * Antworten folgen RFC 7807 ({@link ProblemDetail}). Fachspezifische Domain-Exceptions werden
 * in den jeweiligen Modul-Advices behandelt (z. B. {@code GlobalExceptionHandler},
 * {@code AiExceptionHandler}).
 *
 * <p>Mit {@code @Order(Ordered.LOWEST_PRECEDENCE)} markiert, damit modulspezifische Advices
 * Vorrang behalten — der Catch-all {@code Exception}-Handler hier greift erst, wenn niemand
 * sonst die Exception fängt.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

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

    /**
     * Bean-Validation-Fehler auf {@code @Valid @RequestBody} → 400 mit Feld-Details.
     * Override-Variante der protected Basis-Methode aus {@link ResponseEntityExceptionHandler}
     * (statt einer separaten {@code @ExceptionHandler(MethodArgumentNotValidException.class)}-
     * Methode, die zu „Ambiguous @ExceptionHandler"-Fehlern führen würde).
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(),
                    fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", errors);
        return new ResponseEntity<>(problem, headers, HttpStatus.BAD_REQUEST);
    }

    /**
     * {@link ResponseStatusException} aus Controllern (z. B. {@code throw new ResponseStatusException(NOT_FOUND, …)})
     * → ProblemDetail mit dem angegebenen Status. Spring's Default wandelt ResponseStatusException
     * zwar schon zu einem ProblemDetail, der explizite Handler hier garantiert den
     * `application/problem+json`-Contract über alle Status-Codes.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        String detail = ex.getReason() != null ? ex.getReason() : statusReason(status);
        return ProblemDetail.forStatusAndDetail(status, detail);
    }

    /**
     * Constraint-/FK-/Unique-Verletzungen aus dem Persistenz-Layer → 409 Conflict.
     * <b>Wichtig:</b> Die Original-Message enthält SQL-Schnipsel und Spalten-/Tabellennamen
     * (Information-Leak). Hier wird eine generische Detail-Zeile zurückgegeben und die
     * Originalausnahme intern geloggt.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        LOG.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Datenkonflikt: eine Datenbank-Constraint wurde verletzt (z. B. Unique- oder Foreign-Key-Constraint).");
    }

    /**
     * Spring-Security-`AccessDeniedException` → 403 Forbidden. Ein authentifizierter Nutzer
     * darf den Endpoint formal aufrufen (sonst 401), hat aber nicht die nötige Rolle/Owner-
     * Bindung. {@code AuthenticationException} bleibt bei Springs Default-Filter (→ 401).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Zugriff verweigert.");
    }

    /**
     * Catch-all für echte App-Bugs (alle {@link RuntimeException}-Subtypen, die niemand sonst
     * fängt) → 500 ohne Stack-Trace. Loggt die Original-Exception mit vollem Stack für Ops;
     * die Response enthält nur eine generische Detail-Zeile (kein Klassenname, kein Pfad, kein
     * SQL).
     *
     * <p>Wichtig: bewusst auf {@link RuntimeException} und {@link Error} beschränkt, nicht auf
     * {@link Exception}. Spring's eigene Framework-Exceptions ({@code HttpMessageNotReadableException},
     * {@code HandlerMethodValidationException}, {@code NoResourceFoundException} usw.) erben
     * teils von {@code ServletException} bzw. liefern bereits passende 4xx-Codes über Springs
     * {@code DefaultHandlerExceptionResolver}; ein zu breiter Catch-all würde diese fälsch-
     * licherweise zu 500 ummappen.
     */
    @ExceptionHandler({RuntimeException.class, Error.class})
    public ProblemDetail handleUnexpected(Throwable ex) {
        // Sicherheitsnetz: andere Advices haben Vorrang (höherer @Order) — landet ein
        // RuntimeException trotzdem hier, ist es ein nicht-spezifisch-behandelter App-Bug.
        if (ex instanceof ResponseStatusException rse) {
            return handleResponseStatus(rse);
        }
        if (ex instanceof DataIntegrityViolationException dive) {
            return handleDataIntegrityViolation(dive);
        }
        if (ex instanceof AccessDeniedException ade) {
            return handleAccessDenied(ade);
        }
        if (ex instanceof IllegalArgumentException iae) {
            return handleIllegalArgument(iae);
        }
        LOG.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ein interner Fehler ist aufgetreten. Bitte erneut versuchen oder den Support kontaktieren.");
    }

    private static String statusReason(HttpStatusCode status) {
        if (status instanceof HttpStatus standard) {
            return standard.getReasonPhrase();
        }
        return "Error";
    }
}
