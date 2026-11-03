-- Planning entries at minute precision, in the family's local time. Type-specific columns are
-- NULL for the other types; the service enforces which ones each type requires.
CREATE TABLE planning_entry (
    id                   INTEGER PRIMARY KEY,
    title                TEXT    NOT NULL,
    description          TEXT    NOT NULL DEFAULT '',
    location             TEXT,
    type                 TEXT    NOT NULL CHECK (type IN ('EVENT', 'SHOPPING', 'MEAL', 'ACCOUNTING')),
    start_at             TEXT    NOT NULL,
    end_at               TEXT    NOT NULL,
    all_day              INTEGER NOT NULL DEFAULT 0,
    recurrence_frequency TEXT CHECK (recurrence_frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    recurrence_interval  INTEGER CHECK (recurrence_interval > 0),
    recurrence_days      TEXT,
    recurrence_until     TEXT,
    category             TEXT,
    estimated_cost_cents INTEGER CHECK (estimated_cost_cents >= 0),
    shopping_list_id     INTEGER REFERENCES shopping_list (id) ON DELETE SET NULL,
    recipe_id            INTEGER REFERENCES recipe (id) ON DELETE SET NULL,
    servings             INTEGER CHECK (servings > 0),
    accounting_action    TEXT CHECK (accounting_action IN ('REPORT', 'APPROVAL')),
    done                 INTEGER NOT NULL DEFAULT 0,
    created_at           TEXT    NOT NULL,
    CHECK (end_at >= start_at)
);
CREATE INDEX planning_entry_start ON planning_entry (start_at);
CREATE INDEX planning_entry_shopping_list ON planning_entry (shopping_list_id);

CREATE TABLE planning_entry_member (
    entry_id  INTEGER NOT NULL REFERENCES planning_entry (id) ON DELETE CASCADE,
    member_id INTEGER NOT NULL REFERENCES member (id) ON DELETE CASCADE,
    PRIMARY KEY (entry_id, member_id)
);

-- Dates on which a recurring entry does not take place.
CREATE TABLE planning_exception (
    entry_id INTEGER NOT NULL REFERENCES planning_entry (id) ON DELETE CASCADE,
    day      TEXT    NOT NULL,
    PRIMARY KEY (entry_id, day)
);
