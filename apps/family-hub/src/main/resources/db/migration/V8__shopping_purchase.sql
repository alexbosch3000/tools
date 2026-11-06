-- What the family will actually buy for a row of a list when it differs from what the lines add up
-- to (4 pinches of salt needed, 1 pack bought). A row is an item for a member, or for the household.
CREATE TABLE shopping_purchase (
    id          INTEGER PRIMARY KEY,
    list_id     INTEGER NOT NULL REFERENCES shopping_list (id) ON DELETE CASCADE,
    item_id     INTEGER NOT NULL REFERENCES item (id),
    member_id   INTEGER REFERENCES member (id) ON DELETE CASCADE,
    quantity    REAL    NOT NULL CHECK (quantity > 0),
    unit        TEXT    NOT NULL,
    price_cents INTEGER CHECK (price_cents >= 0),
    note        TEXT
);
CREATE UNIQUE INDEX shopping_purchase_row ON shopping_purchase (list_id, item_id, IFNULL(member_id, 0));
