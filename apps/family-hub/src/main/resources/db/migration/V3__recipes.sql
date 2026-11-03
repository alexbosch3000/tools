-- Cooking recipes. Quantities are given for `servings` people and scaled on use.
CREATE TABLE recipe (
    id           INTEGER PRIMARY KEY,
    name         TEXT    NOT NULL,
    servings     INTEGER NOT NULL CHECK (servings > 0),
    prep_minutes INTEGER CHECK (prep_minutes >= 0),
    cook_minutes INTEGER CHECK (cook_minutes >= 0),
    instructions TEXT    NOT NULL DEFAULT '',
    tags         TEXT    NOT NULL DEFAULT '',
    source_url   TEXT,
    created_at   TEXT    NOT NULL
);

CREATE TABLE recipe_ingredient (
    id        INTEGER PRIMARY KEY,
    recipe_id INTEGER NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    item_id   INTEGER NOT NULL REFERENCES item (id),
    quantity  REAL    NOT NULL CHECK (quantity > 0),
    unit      TEXT    NOT NULL,
    note      TEXT,
    position  INTEGER NOT NULL
);
CREATE INDEX recipe_ingredient_recipe ON recipe_ingredient (recipe_id);
CREATE INDEX recipe_ingredient_item ON recipe_ingredient (item_id);

-- Three classics so that the recipe and shopping features can be tried right away.
INSERT INTO recipe (name, servings, prep_minutes, cook_minutes, instructions, tags, created_at) VALUES
    ('Pâtes carbonara', 4, 10, 15,
     'Cuire les pâtes. Faire revenir les lardons. Mélanger les œufs et le parmesan, ajouter hors du feu aux pâtes et aux lardons. Poivrer.',
     'rapide,pâtes', '2026-01-01T00:00'),
    ('Ratatouille', 4, 25, 45,
     'Couper les légumes en dés. Faire revenir l''oignon et l''ail dans l''huile, ajouter les légumes, les herbes et les tomates. Mijoter 45 minutes.',
     'végétarien,légumes', '2026-01-01T00:00'),
    ('Crêpes', 4, 10, 20,
     'Mélanger la farine et le sucre, ajouter les œufs puis le lait progressivement et le beurre fondu. Laisser reposer une heure. Cuire à la poêle.',
     'dessert,goûter', '2026-01-01T00:00');

WITH v (recipe, item, quantity, unit, position) AS (VALUES
        ('Pâtes carbonara', 'Pâtes', 400, 'G', 1),
        ('Pâtes carbonara', 'Lardons', 200, 'G', 2),
        ('Pâtes carbonara', 'Œufs', 4, 'PIECE', 3),
        ('Pâtes carbonara', 'Parmesan', 80, 'G', 4),
        ('Pâtes carbonara', 'Poivre', 1, 'PINCH', 5),
        ('Ratatouille', 'Courgettes', 500, 'G', 1),
        ('Ratatouille', 'Aubergines', 400, 'G', 2),
        ('Ratatouille', 'Poivrons', 300, 'G', 3),
        ('Ratatouille', 'Tomates', 500, 'G', 4),
        ('Ratatouille', 'Oignons', 1, 'PIECE', 5),
        ('Ratatouille', 'Ail', 2, 'PIECE', 6),
        ('Ratatouille', 'Huile d''olive', 4, 'TBSP', 7),
        ('Ratatouille', 'Herbes de Provence', 1, 'TSP', 8),
        ('Crêpes', 'Farine', 250, 'G', 1),
        ('Crêpes', 'Œufs', 4, 'PIECE', 2),
        ('Crêpes', 'Lait', 50, 'CL', 3),
        ('Crêpes', 'Sucre', 2, 'TBSP', 4),
        ('Crêpes', 'Beurre', 50, 'G', 5))
INSERT INTO recipe_ingredient (recipe_id, item_id, quantity, unit, position)
SELECT r.id, i.id, v.quantity, v.unit, v.position
FROM v
JOIN recipe r ON r.name = v.recipe
JOIN item i ON i.name = v.item;
