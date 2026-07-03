-- TEN-59: Audit-Spalten auf Player, Match, Point.
-- Spalten sind nullable: Flyway-Migrations und scheduled-Jobs ohne Auth-Context
-- dürfen NULL hinterlassen. Hibernate setzt sie bei neuen Inserts via Spring Data Auditing.

ALTER TABLE players ADD COLUMN created_at TIMESTAMP;
ALTER TABLE players ADD COLUMN created_by UUID;
ALTER TABLE players ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE players ADD COLUMN updated_by UUID;

ALTER TABLE matches ADD COLUMN created_at TIMESTAMP;
ALTER TABLE matches ADD COLUMN created_by UUID;
ALTER TABLE matches ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE matches ADD COLUMN updated_by UUID;

ALTER TABLE points  ADD COLUMN created_at TIMESTAMP;
ALTER TABLE points  ADD COLUMN created_by UUID;
ALTER TABLE points  ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE points  ADD COLUMN updated_by UUID;

-- Backfill: best-effort attribution from owner.
UPDATE players SET created_by = owner_id, created_at = NOW(), updated_at = NOW();
UPDATE matches SET created_by = owner_id, created_at = NOW(), updated_at = NOW();
UPDATE points p
   SET created_at = NOW(),
       updated_at = NOW(),
       created_by = (SELECT m.owner_id FROM matches m WHERE m.id = p.match_id);
