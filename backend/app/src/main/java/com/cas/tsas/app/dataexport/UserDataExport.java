package com.cas.tsas.app.dataexport;

import com.cas.tsas.ai.infrastructure.persistence.entity.MatchAnalysisJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.MatchScoreJpaEntity;
import com.cas.tsas.match.infrastructure.persistence.entity.PointJpaEntity;
import com.cas.tsas.player.infrastructure.persistence.entity.PlayerJpaEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full snapshot of a user's data — DSGVO Art. 20 (right to data portability).
 *
 * <p>The aggregates are serialised straight out of their JPA entities; this keeps the export
 * format aligned with what is actually persisted (no hidden state). The header carries the
 * provenance information that lets a recipient identify the export's owner and the moment of
 * issuance — recommended by Art. 20 commentary literature.
 */
public record UserDataExport(
        Header header,
        List<PlayerJpaEntity> players,
        List<MatchJpaEntity> matches,
        List<PointJpaEntity> points,
        List<MatchScoreJpaEntity> scores,
        List<MatchAnalysisJpaEntity> analyses
) {

    /** Provenance information for the export — userId, when, in which format. */
    public record Header(UUID userId, Instant exportedAt, String format) {

        public static Header now(UUID userId) {
            return new Header(userId, Instant.now(), "application/json; profile=dsgvo-art20-v1");
        }
    }
}
