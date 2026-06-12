/**
 * AI module — AI-assisted tactical match analysis.
 *
 * <p>Consumes the {@code statistics-module} (aggregated stats) and {@code player-module}
 * (player metadata as context), invokes an LLM through the {@link com.cas.tsas.ai.application.port.out.LlmClientPort}
 * (default OpenAI via Spring AI; a deterministic fake is used when no API key is configured)
 * and persists the result as a {@link com.cas.tsas.ai.domain.model.MatchAnalysis} (1:1 to the
 * match, overwritable). REST: {@code POST/GET /api/matches/{id}/analysis}.
 *
 * <p>Structured along Clean Architecture layers (infrastructure → application → domain).
 * Module dependencies: {@code common-module}, {@code match-module}, {@code player-module},
 * {@code statistics-module}; behaviour is invoked through their application-layer ports while
 * domain value types are reused as a shared read model (see ADR-13).
 */
package com.cas.tsas.ai;
