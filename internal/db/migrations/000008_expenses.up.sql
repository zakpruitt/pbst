CREATE TABLE expenses (
    id            BIGSERIAL PRIMARY KEY,
    name          TEXT NOT NULL DEFAULT '',
    expense_date  TIMESTAMPTZ,
    cost          NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

CREATE INDEX idx_expenses_deleted_at  ON expenses(deleted_at);
CREATE INDEX idx_expenses_expense_date ON expenses(expense_date);
