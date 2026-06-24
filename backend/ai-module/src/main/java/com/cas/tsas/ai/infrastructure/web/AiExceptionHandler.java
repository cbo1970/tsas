package com.cas.tsas.ai.infrastructure.web;

import com.cas.tsas.ai.domain.exception.AnalysisGenerationException;
import com.cas.tsas.ai.domain.exception.InsufficientHeadToHeadDataException;
import com.cas.tsas.ai.domain.exception.InsufficientMatchDataException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AiExceptionHandler {

    @ExceptionHandler(InsufficientMatchDataException.class)
    public ProblemDetail handleInsufficient(InsufficientMatchDataException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(InsufficientHeadToHeadDataException.class)
    public ProblemDetail handleInsufficientH2h(InsufficientHeadToHeadDataException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(AnalysisGenerationException.class)
    public ProblemDetail handleAnalysisGeneration(AnalysisGenerationException ex) {
        String detail = ex.getCause() != null
                ? ex.getMessage() + " — " + ex.getCause().getMessage()
                : ex.getMessage();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, detail);
    }
}
