/**
 * Match module — creating and managing matches (Begegnungen) and recording the running
 * score point by point.
 *
 * <p>This module also owns the <strong>scoring</strong> concern (tennis counting rules in
 * {@code ScoringService}, point capture, the {@link com.cas.tsas.match.domain.model.MatchScore}
 * and {@link com.cas.tsas.match.domain.model.Point} entities). The SAD originally sketched a
 * separate {@code scoring-module}; scoring was consolidated here because it is the core
 * behaviour of a match and is tightly coupled to its state (see ADR-12).
 *
 * <p>Structured along Clean Architecture layers (infrastructure → application → domain).
 * Module dependencies: {@code common-module} and {@code player-module}; player data is
 * accessed only through player's application-layer ports.
 */
package com.cas.tsas.match;
