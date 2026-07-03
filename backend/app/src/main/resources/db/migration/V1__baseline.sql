CREATE TABLE players (
    id            UUID         NOT NULL,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255) NOT NULL,
    gender        VARCHAR(255),
    handedness    VARCHAR(255),
    backhand_type VARCHAR(255),
    ranking       VARCHAR(255),
    nationality   VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    birth_date    DATE,
    PRIMARY KEY (id)
);

CREATE TABLE matches (
    id             UUID         NOT NULL,
    player1_id     UUID         NOT NULL,
    player2_id     UUID         NOT NULL,
    sets_to_win    INTEGER      NOT NULL,
    match_tiebreak BOOLEAN      NOT NULL,
    short_set      BOOLEAN      NOT NULL,
    status         VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_matches_player1 FOREIGN KEY (player1_id) REFERENCES players(id),
    CONSTRAINT fk_matches_player2 FOREIGN KEY (player2_id) REFERENCES players(id)
);

CREATE TABLE match_scores (
    id                   UUID         NOT NULL,
    match_id             UUID         NOT NULL,
    points_player1       INTEGER      NOT NULL DEFAULT 0,
    points_player2       INTEGER      NOT NULL DEFAULT 0,
    games_player1        INTEGER      NOT NULL DEFAULT 0,
    games_player2        INTEGER      NOT NULL DEFAULT 0,
    sets_player1         INTEGER      NOT NULL DEFAULT 0,
    sets_player2         INTEGER      NOT NULL DEFAULT 0,
    is_deuce             BOOLEAN      NOT NULL DEFAULT FALSE,
    is_advantage_player1 BOOLEAN,
    current_set          INTEGER      NOT NULL DEFAULT 1,
    is_done              BOOLEAN      NOT NULL DEFAULT FALSE,
    winner               VARCHAR(255),
    aces_player1         INTEGER      NOT NULL DEFAULT 0,
    aces_player2         INTEGER      NOT NULL DEFAULT 0,
    serving_player       INTEGER,
    PRIMARY KEY (id),
    UNIQUE (match_id),
    CONSTRAINT fk_match_scores_match FOREIGN KEY (match_id) REFERENCES matches(id)
);
