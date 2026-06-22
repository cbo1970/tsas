-- TEN-55: Owner-Binding auf Player und Match.
-- Backfill-UUID 00000000-... markiert Pre-Migration-Daten (Dev-Bestand).
-- Prod startet ohne Datenbestand; in Prod ist der Backfill no-op.

ALTER TABLE players ADD COLUMN owner_id UUID;
ALTER TABLE matches ADD COLUMN owner_id UUID;

UPDATE players SET owner_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id IS NULL;
UPDATE matches SET owner_id = '00000000-0000-0000-0000-000000000000' WHERE owner_id IS NULL;

ALTER TABLE players ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE matches ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX idx_players_owner ON players(owner_id);
CREATE INDEX idx_matches_owner ON matches(owner_id);
