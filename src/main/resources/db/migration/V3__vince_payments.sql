CREATE TABLE IF NOT EXISTS vince_payments (
    id           BIGSERIAL PRIMARY KEY,
    amount       NUMERIC(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    description  TEXT NOT NULL DEFAULT '',
    type         TEXT NOT NULL DEFAULT 'PAYOUT',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_vince_payments_date ON vince_payments(payment_date);
