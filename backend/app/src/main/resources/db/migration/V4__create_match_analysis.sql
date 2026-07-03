CREATE TABLE match_analysis (
    id                  UUID         NOT NULL,
    match_id            UUID         NOT NULL UNIQUE,
    status              VARCHAR(16)  NOT NULL,
    key_moments         TEXT,
    own_strengths       TEXT,
    own_weaknesses      TEXT,
    opponent_strengths  TEXT,
    opponent_weaknesses TEXT,
    recommendations     TEXT         NOT NULL DEFAULT '[]',
    model_used          VARCHAR(64),
    error_message       TEXT,
    generated_at        TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_match_analysis_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX idx_match_analysis_match ON match_analysis(match_id);
