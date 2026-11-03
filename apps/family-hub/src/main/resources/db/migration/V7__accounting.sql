-- Family budget: a single-entry ledger with envelope budgets per category.
-- Amounts are integer euro cents. The direction of a transaction is the one of its category.
CREATE TABLE account (
    id                    INTEGER PRIMARY KEY,
    name                  TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    kind                  TEXT    NOT NULL CHECK (kind IN ('CHECKING', 'SAVINGS', 'CASH', 'CARD')),
    opening_balance_cents INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE budget_category (
    code                 TEXT PRIMARY KEY,
    name                 TEXT    NOT NULL,
    direction            TEXT    NOT NULL CHECK (direction IN ('INCOME', 'EXPENSE')),
    budget_group         TEXT CHECK (budget_group IN ('NEEDS', 'WANTS', 'SAVINGS')),
    monthly_budget_cents INTEGER CHECK (monthly_budget_cents >= 0),
    position             INTEGER NOT NULL,
    CHECK ((direction = 'INCOME') = (budget_group IS NULL))
);

CREATE TABLE ledger_transaction (
    id            INTEGER PRIMARY KEY,
    day           TEXT    NOT NULL,
    label         TEXT    NOT NULL,
    amount_cents  INTEGER NOT NULL CHECK (amount_cents >= 0),
    category_code TEXT    NOT NULL REFERENCES budget_category (code),
    account_id    INTEGER REFERENCES account (id),
    status        TEXT    NOT NULL CHECK (status IN ('PLANNED', 'PENDING', 'APPROVED')),
    source_type   TEXT    NOT NULL CHECK (source_type IN ('MANUAL', 'PLANNING', 'SHOPPING', 'RECURRING')),
    source_ref    TEXT,
    supplier      TEXT,
    created_by    INTEGER REFERENCES member (id) ON DELETE SET NULL,
    approved_by   INTEGER REFERENCES member (id) ON DELETE SET NULL,
    approved_at   TEXT,
    created_at    TEXT    NOT NULL,
    UNIQUE (source_type, source_ref)
);
CREATE INDEX ledger_transaction_day ON ledger_transaction (day);

-- Bills, subscriptions and regular income; each generates PLANNED transactions month by month.
CREATE TABLE recurring_transaction (
    id            INTEGER PRIMARY KEY,
    label         TEXT    NOT NULL,
    category_code TEXT    NOT NULL REFERENCES budget_category (code),
    supplier      TEXT,
    amount_cents  INTEGER NOT NULL CHECK (amount_cents >= 0),
    period        TEXT    NOT NULL CHECK (period IN ('MONTHLY', 'BIMONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'YEARLY')),
    day_of_month  INTEGER NOT NULL CHECK (day_of_month BETWEEN 1 AND 31),
    start_date    TEXT    NOT NULL,
    end_date      TEXT,
    account_id    INTEGER REFERENCES account (id),
    active        INTEGER NOT NULL DEFAULT 1
);

INSERT INTO account (name, kind) VALUES ('Compte joint', 'CHECKING');

-- Envelopes grouped along the 50/30/20 rule: needs, wants, savings. Budgets are left to the family.
INSERT INTO budget_category (code, name, direction, budget_group, position) VALUES
    ('salaires', 'Salaires', 'INCOME', NULL, 1),
    ('apport-mensuel', 'Apport mensuel', 'INCOME', NULL, 2),
    ('allocations', 'Allocations et aides', 'INCOME', NULL, 3),
    ('autres-revenus', 'Autres revenus', 'INCOME', NULL, 4),
    ('logement', 'Logement', 'EXPENSE', 'NEEDS', 10),
    ('courses', 'Courses alimentaires', 'EXPENSE', 'NEEDS', 11),
    ('energie', 'Énergie (électricité, gaz)', 'EXPENSE', 'NEEDS', 12),
    ('eau', 'Eau', 'EXPENSE', 'NEEDS', 13),
    ('telecom', 'Télécom (internet, mobile)', 'EXPENSE', 'NEEDS', 14),
    ('assurances', 'Assurances', 'EXPENSE', 'NEEDS', 15),
    ('transport', 'Transport', 'EXPENSE', 'NEEDS', 16),
    ('sante', 'Santé', 'EXPENSE', 'NEEDS', 17),
    ('impots', 'Impôts', 'EXPENSE', 'NEEDS', 18),
    ('scolarite', 'Scolarité', 'EXPENSE', 'NEEDS', 19),
    ('garde', 'Garde et centre aéré', 'EXPENSE', 'NEEDS', 20),
    ('maison', 'Maison, bricolage, jardin', 'EXPENSE', 'WANTS', 30),
    ('sport', 'Sport', 'EXPENSE', 'WANTS', 31),
    ('loisirs', 'Loisirs et sorties', 'EXPENSE', 'WANTS', 32),
    ('vacances', 'Vacances', 'EXPENSE', 'WANTS', 33),
    ('cadeaux', 'Cadeaux', 'EXPENSE', 'WANTS', 34),
    ('divers', 'Divers', 'EXPENSE', 'WANTS', 35),
    ('epargne', 'Épargne', 'EXPENSE', 'SAVINGS', 40);
