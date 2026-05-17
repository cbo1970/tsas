package com.cas.tsas.ai.domain.exception;

public class AnalysisGenerationException extends RuntimeException {

    public AnalysisGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AnalysisGenerationException(String message) {
        super(message);
    }
}
