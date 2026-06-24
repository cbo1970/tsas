-- TEN-6: Persistenz der Spracheinstellung pro Nutzer.
-- Der Primary-Key ist die Keycloak-UUID (`sub` aus dem JWT) — keine separate Surrogate-ID,
-- da ein Nutzer immer genau eine Präferenz hat. Default 'de', um Bestandsnutzern ohne
-- Eintrag deterministisch Deutsch zu liefern.
CREATE TABLE user_preferences (
    user_id      UUID PRIMARY KEY,
    language     VARCHAR(2) NOT NULL DEFAULT 'de',
    updated_at   TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_preferences_language_check CHECK (language IN ('de', 'en', 'it', 'fr'))
);
