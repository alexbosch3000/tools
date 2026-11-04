-- Shopping lists. Lines either come from the family directly (recipe_entry_id NULL) or from a
-- recipe added to the list, in which case removing the recipe removes its lines. A line can be
-- bought for a given member (clothes, shoes), in which case it is kept apart from the same item for
-- someone else.
CREATE TABLE shopping_list (
    id                INTEGER PRIMARY KEY,
    name              TEXT    NOT NULL,
    status            TEXT    NOT NULL CHECK (status IN ('OPEN', 'DONE')),
    planned_for       TEXT,
    completed_on      TEXT,
    amount_paid_cents INTEGER CHECK (amount_paid_cents >= 0),
    created_at        TEXT    NOT NULL
);

CREATE TABLE shopping_list_recipe (
    id        INTEGER PRIMARY KEY,
    list_id   INTEGER NOT NULL REFERENCES shopping_list (id) ON DELETE CASCADE,
    recipe_id INTEGER NOT NULL REFERENCES recipe (id),
    servings  INTEGER NOT NULL CHECK (servings > 0)
);
CREATE INDEX shopping_list_recipe_list ON shopping_list_recipe (list_id);

CREATE TABLE shopping_line (
    id              INTEGER PRIMARY KEY,
    list_id         INTEGER NOT NULL REFERENCES shopping_list (id) ON DELETE CASCADE,
    item_id         INTEGER NOT NULL REFERENCES item (id),
    recipe_entry_id INTEGER REFERENCES shopping_list_recipe (id) ON DELETE CASCADE,
    member_id       INTEGER REFERENCES member (id) ON DELETE SET NULL,
    quantity        REAL    NOT NULL CHECK (quantity > 0),
    unit            TEXT    NOT NULL,
    checked         INTEGER NOT NULL DEFAULT 0,
    note            TEXT
);
CREATE INDEX shopping_line_list ON shopping_line (list_id);
CREATE INDEX shopping_line_item ON shopping_line (item_id);
