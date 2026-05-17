ALTER TABLE points ADD COLUMN serve_attempt INTEGER;
ALTER TABLE points ADD CONSTRAINT chk_serve_attempt
    CHECK (serve_attempt IS NULL OR serve_attempt IN (1, 2));
