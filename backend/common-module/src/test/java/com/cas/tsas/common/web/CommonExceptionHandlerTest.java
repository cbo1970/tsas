package com.cas.tsas.common.web;

import com.cas.tsas.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit-tests for the global error handlers added by TEN-61 / STRIDE I5+I6. */
class CommonExceptionHandlerTest {

    private final CommonExceptionHandler handler = new CommonExceptionHandler();

    @Test
    void responseStatusException_keepsStatusAndReason() {
        ProblemDetail body = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Spieler nicht gefunden"));
        assertEquals(404, body.getStatus());
        assertEquals("Spieler nicht gefunden", body.getDetail());
    }

    @Test
    void responseStatusException_fillsDetailWithStandardReasonIfNoneGiven() {
        ProblemDetail body = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertEquals(403, body.getStatus());
        assertEquals("Forbidden", body.getDetail());
    }

    @Test
    void dataIntegrityViolation_returns409AndSanitisesDetail() {
        SQLException sqlEx = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"match_analysis_match_id_key\" "
                        + "Detail: Key (match_id)=(00000000-0000-0000-0000-000000000001) already exists.");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("wrap", sqlEx);

        ProblemDetail body = handler.handleDataIntegrityViolation(ex);

        assertEquals(409, body.getStatus());
        assertNotNull(body.getDetail());
        // Critical: detail must NOT leak the SQL, the column name, or the key value.
        assertFalse(body.getDetail().contains("SQL"));
        assertFalse(body.getDetail().contains("match_analysis_match_id_key"));
        assertFalse(body.getDetail().contains("00000000"));
        assertFalse(body.getDetail().toLowerCase().contains("error:"));
        assertTrue(body.getDetail().toLowerCase().contains("constraint"),
                () -> "expected a generic constraint message but got: " + body.getDetail());
    }

    @Test
    void accessDenied_returns403WithGenericDetail() {
        ProblemDetail body = handler.handleAccessDenied(new AccessDeniedException("any reason"));
        assertEquals(403, body.getStatus());
        assertNotNull(body.getDetail());
        // The internal exception message must NOT be passed through to the response.
        assertFalse(body.getDetail().contains("any reason"));
    }

    @Test
    void catchAll_returns500AndDoesNotLeakStackOrClassName() {
        RuntimeException ex = new RuntimeException("connection refused to internal-host:5432");
        ProblemDetail body = handler.handleUnexpected(ex);
        assertEquals(500, body.getStatus());
        assertNotNull(body.getDetail());
        assertFalse(body.getDetail().contains("RuntimeException"));
        assertFalse(body.getDetail().contains("internal-host"));
        assertFalse(body.getDetail().contains("5432"));
    }

    @Test
    void conflictException_unchangedBehaviour() {
        ProblemDetail body = handler.handleConflict(new ConflictException("Spieler bereits aktiv"));
        assertEquals(409, body.getStatus());
        assertEquals("Spieler bereits aktiv", body.getDetail());
    }

    @Test
    void illegalArgument_returns400WithMessage() {
        ProblemDetail body = handler.handleIllegalArgument(
                new IllegalArgumentException("Unknown pointType: BANANA"));
        assertEquals(400, body.getStatus());
        assertEquals("Unknown pointType: BANANA", body.getDetail());
    }
}
