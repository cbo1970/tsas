-- TEN-68: Coach-Freitext-Notizen je Spieler und Match (eine Notiz pro (match, player)).
-- created_by/updated_by sind UUID (Keycloak-sub), konsistent mit V7-Audit-Spalten; nullable,
-- damit Flyway/Jobs ohne Auth-Context NULL hinterlassen duerfen.
CREATE TABLE match_player_notes (
    id          UUID          NOT NULL,
    match_id    UUID          NOT NULL,
    player_id   UUID          NOT NULL,
    note        VARCHAR(2000) NOT NULL,
    created_at  TIMESTAMP,
    created_by  UUID,
    updated_at  TIMESTAMP,
    updated_by  UUID,
    PRIMARY KEY (id),
    CONSTRAINT uq_match_player_notes UNIQUE (match_id, player_id),
    CONSTRAINT fk_match_player_notes_match  FOREIGN KEY (match_id)  REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_player_notes_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_match_player_notes_player ON match_player_notes(player_id);
