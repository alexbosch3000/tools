-- Family members. A member without username cannot log in and only appears as an assignee.
-- Sizes are free text ("8 ans", "M", "32") shown when buying clothes for the member.
CREATE TABLE member (
    id                  INTEGER PRIMARY KEY,
    name                TEXT    NOT NULL,
    color               TEXT    NOT NULL,
    role                TEXT    NOT NULL CHECK (role IN ('PARENT', 'CHILD')),
    username            TEXT    UNIQUE COLLATE NOCASE,
    password_hash       TEXT,
    clothing_size       TEXT,
    shoe_size           TEXT,
    calendar_token_hash TEXT    UNIQUE,
    created_at          TEXT    NOT NULL
);

-- API access tokens issued at login. Only their SHA-256 hash is stored; the expiry slides with use.
CREATE TABLE auth_token (
    id         INTEGER PRIMARY KEY,
    member_id  INTEGER NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    token_hash TEXT    NOT NULL UNIQUE,
    created_at TEXT    NOT NULL,
    expires_at TEXT    NOT NULL
);
CREATE INDEX auth_token_member ON auth_token (member_id);
