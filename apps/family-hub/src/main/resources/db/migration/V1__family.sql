-- Family members. A member without username cannot log in and only appears as an assignee.
CREATE TABLE member (
    id                  INTEGER PRIMARY KEY,
    name                TEXT    NOT NULL,
    color               TEXT    NOT NULL,
    role                TEXT    NOT NULL CHECK (role IN ('PARENT', 'CHILD')),
    username            TEXT    UNIQUE COLLATE NOCASE,
    password_hash       TEXT,
    calendar_token_hash TEXT    UNIQUE,
    created_at          TEXT    NOT NULL
);
