-- V8: Backfill NULLs in match_scores and re-assert NOT NULL on every column whose
-- Hibernate counterpart is a primitive (int / boolean).
--
-- Hintergrund: Die V1-Baseline deklariert die Spalten korrekt als NOT NULL, in DBs,
-- die vor V1 aus Hibernate's auto-DDL erzeugt wurden, fehlt der Constraint jedoch
-- (Hibernate mapped primitive Felder ohne @Column(nullable=false) als nullable). V1
-- wurde damals nur in flyway_schema_history als „applied" markiert, der Constraint
-- aber nicht nachträglich gesetzt. Folge: `int`-Felder im JPA-Entity können beim
-- Laden auf NULL stossen → IllegalArgumentException → 400. Aufgefallen beim
-- TEN-66-Datenexport.

UPDATE match_scores SET points_player1 = 0 WHERE points_player1 IS NULL;
UPDATE match_scores SET points_player2 = 0 WHERE points_player2 IS NULL;
UPDATE match_scores SET games_player1  = 0 WHERE games_player1  IS NULL;
UPDATE match_scores SET games_player2  = 0 WHERE games_player2  IS NULL;
UPDATE match_scores SET sets_player1   = 0 WHERE sets_player1   IS NULL;
UPDATE match_scores SET sets_player2   = 0 WHERE sets_player2   IS NULL;
UPDATE match_scores SET is_deuce       = FALSE WHERE is_deuce   IS NULL;
UPDATE match_scores SET current_set    = 1 WHERE current_set    IS NULL;
UPDATE match_scores SET is_done        = FALSE WHERE is_done    IS NULL;
UPDATE match_scores SET aces_player1   = 0 WHERE aces_player1   IS NULL;
UPDATE match_scores SET aces_player2   = 0 WHERE aces_player2   IS NULL;

ALTER TABLE match_scores ALTER COLUMN points_player1 SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN points_player2 SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN games_player1  SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN games_player2  SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN sets_player1   SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN sets_player2   SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN is_deuce       SET DEFAULT FALSE;
ALTER TABLE match_scores ALTER COLUMN current_set    SET DEFAULT 1;
ALTER TABLE match_scores ALTER COLUMN is_done        SET DEFAULT FALSE;
ALTER TABLE match_scores ALTER COLUMN aces_player1   SET DEFAULT 0;
ALTER TABLE match_scores ALTER COLUMN aces_player2   SET DEFAULT 0;

ALTER TABLE match_scores ALTER COLUMN points_player1 SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN points_player2 SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN games_player1  SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN games_player2  SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN sets_player1   SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN sets_player2   SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN is_deuce       SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN current_set    SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN is_done        SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN aces_player1   SET NOT NULL;
ALTER TABLE match_scores ALTER COLUMN aces_player2   SET NOT NULL;
