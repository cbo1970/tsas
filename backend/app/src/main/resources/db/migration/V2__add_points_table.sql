CREATE TABLE points (
    id             UUID         NOT NULL,
    match_id       UUID         NOT NULL,
    set_number     SMALLINT     NOT NULL,
    game_number    SMALLINT     NOT NULL,
    point_number   SMALLINT     NOT NULL,
    winner         SMALLINT     NOT NULL,
    point_type     VARCHAR(50)  NOT NULL,
    stroke_type    VARCHAR(50),
    direction      VARCHAR(50),
    serving_player SMALLINT,
    is_break_point BOOLEAN      NOT NULL DEFAULT FALSE,
    remark         VARCHAR(500),
    recorded_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id),
    CONSTRAINT fk_points_match FOREIGN KEY (match_id) REFERENCES matches(id)
);

CREATE INDEX idx_points_match_id ON points(match_id);
