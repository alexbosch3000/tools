-- To-do lists. A repeating item spawns its next occurrence when it is completed.
CREATE TABLE todo_list (
    id         INTEGER PRIMARY KEY,
    name       TEXT NOT NULL UNIQUE COLLATE NOCASE,
    created_at TEXT NOT NULL
);

CREATE TABLE todo_item (
    id          INTEGER PRIMARY KEY,
    list_id     INTEGER NOT NULL REFERENCES todo_list (id) ON DELETE CASCADE,
    title       TEXT    NOT NULL,
    notes       TEXT    NOT NULL DEFAULT '',
    assignee_id INTEGER REFERENCES member (id) ON DELETE SET NULL,
    due_at      TEXT,
    priority    TEXT    NOT NULL CHECK (priority IN ('LOW', 'NORMAL', 'HIGH')),
    repeat      TEXT CHECK (repeat IN ('DAILY', 'WEEKLY', 'MONTHLY')),
    done        INTEGER NOT NULL DEFAULT 0,
    done_at     TEXT,
    created_at  TEXT    NOT NULL
);
CREATE INDEX todo_item_list ON todo_item (list_id);

INSERT INTO todo_list (name, created_at) VALUES
    ('Maison', '2026-01-01T00:00'),
    ('Administratif', '2026-01-01T00:00'),
    ('École', '2026-01-01T00:00');
