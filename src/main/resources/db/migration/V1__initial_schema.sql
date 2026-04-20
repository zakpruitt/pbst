CREATE TABLE IF NOT EXISTS pokemon_cards (
    id             TEXT PRIMARY KEY,
    name           TEXT NOT NULL DEFAULT '',
    set_code       TEXT NOT NULL DEFAULT '',
    set_name       TEXT NOT NULL DEFAULT '',
    card_number    TEXT NOT NULL DEFAULT '',
    rarity         TEXT NOT NULL DEFAULT '',
    image_url      TEXT NOT NULL DEFAULT '',
    market_price   NUMERIC(10,2) NOT NULL DEFAULT 0,
    low_price      NUMERIC(10,2) NOT NULL DEFAULT 0,
    last_price_sync TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS sealed_products (
    id             TEXT PRIMARY KEY,
    name           TEXT NOT NULL DEFAULT '',
    set_code       TEXT NOT NULL DEFAULT '',
    set_name       TEXT NOT NULL DEFAULT '',
    image_url      TEXT NOT NULL DEFAULT '',
    market_price   NUMERIC(10,2) NOT NULL DEFAULT 0,
    low_price      NUMERIC(10,2) NOT NULL DEFAULT 0,
    last_price_sync TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS lot_purchases (
    id                      BIGSERIAL PRIMARY KEY,
    seller_name             TEXT NOT NULL DEFAULT '',
    purchase_date           TIMESTAMPTZ,
    description             TEXT NOT NULL DEFAULT '',
    total_cost              NUMERIC(10,2) NOT NULL DEFAULT 0,
    estimated_market_value  NUMERIC(10,2) NOT NULL DEFAULT 0,
    lot_content_snapshot    TEXT NOT NULL DEFAULT '',
    status                  TEXT NOT NULL DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS grading_submissions (
    id                BIGSERIAL PRIMARY KEY,
    submission_name   TEXT NOT NULL DEFAULT '',
    send_date         TIMESTAMPTZ,
    return_date       TIMESTAMPTZ,
    company           TEXT NOT NULL DEFAULT '',
    status            TEXT NOT NULL DEFAULT 'PREPPING',
    submission_method TEXT NOT NULL DEFAULT '',
    notes             TEXT,
    upcharge_total    NUMERIC(10,2) NOT NULL DEFAULT 0,
    cost_per_card     NUMERIC(10,2) NOT NULL DEFAULT 20.00,
    tax_rate          NUMERIC(6,5) NOT NULL DEFAULT 0.04225,
    submission_cost   NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS sales (
    id             BIGSERIAL PRIMARY KEY,
    ebay_order_id  TEXT UNIQUE,
    sale_date      TIMESTAMPTZ,
    title          TEXT NOT NULL DEFAULT '',
    buyer_username TEXT NOT NULL DEFAULT '',
    gross_amount   NUMERIC(10,2) NOT NULL DEFAULT 0,
    ebay_fees      NUMERIC(10,2) NOT NULL DEFAULT 0,
    shipping_cost  NUMERIC(10,2) NOT NULL DEFAULT 0,
    net_amount     NUMERIC(10,2) NOT NULL DEFAULT 0,
    image_url      TEXT NOT NULL DEFAULT '',
    order_status   TEXT NOT NULL DEFAULT '',
    origin         TEXT NOT NULL DEFAULT 'EBAY',
    status         TEXT NOT NULL DEFAULT 'STAGED',
    attributed_to  TEXT NOT NULL DEFAULT '',
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tracked_items (
    id                      BIGSERIAL PRIMARY KEY,
    acquisition_date        TIMESTAMPTZ,
    cost_basis              NUMERIC(10,2) NOT NULL DEFAULT 0,
    market_value_at_purchase NUMERIC(10,2) NOT NULL DEFAULT 0,
    manual_name_override    TEXT,
    notes                   TEXT,
    purpose                 TEXT NOT NULL DEFAULT 'INVENTORY',
    item_type               TEXT NOT NULL DEFAULT 'RAW_CARD',
    lot_purchase_id         BIGINT REFERENCES lot_purchases(id),
    pokemon_card_id         TEXT REFERENCES pokemon_cards(id),
    sealed_product_id       TEXT REFERENCES sealed_products(id),
    grading_submission_id   BIGINT REFERENCES grading_submissions(id),
    sale_id                 BIGINT REFERENCES sales(id),
    grading_company         TEXT,
    grade                   TEXT,
    grading_upcharge        NUMERIC(10,2),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS expenses (
    id            BIGSERIAL PRIMARY KEY,
    name          TEXT NOT NULL DEFAULT '',
    expense_date  TIMESTAMPTZ,
    cost          NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMPTZ
);

-- Indexes (IF NOT EXISTS requires Postgres 9.5+)
CREATE INDEX IF NOT EXISTS idx_lot_purchases_deleted_at       ON lot_purchases(deleted_at);
CREATE INDEX IF NOT EXISTS idx_tracked_items_deleted_at       ON tracked_items(deleted_at);
CREATE INDEX IF NOT EXISTS idx_grading_submissions_deleted_at ON grading_submissions(deleted_at);
CREATE INDEX IF NOT EXISTS idx_sales_deleted_at               ON sales(deleted_at);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_deleted_at       ON pokemon_cards(deleted_at);
CREATE INDEX IF NOT EXISTS idx_sealed_products_deleted_at     ON sealed_products(deleted_at);
CREATE INDEX IF NOT EXISTS idx_expenses_deleted_at            ON expenses(deleted_at);
CREATE INDEX IF NOT EXISTS idx_tracked_items_purpose          ON tracked_items(purpose);
CREATE INDEX IF NOT EXISTS idx_tracked_items_lot_purchase_id  ON tracked_items(lot_purchase_id);
CREATE INDEX IF NOT EXISTS idx_tracked_items_sale_id          ON tracked_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_pokemon_cards_name             ON pokemon_cards(name);
CREATE INDEX IF NOT EXISTS idx_sales_status                   ON sales(status);
CREATE INDEX IF NOT EXISTS idx_sales_attributed_to            ON sales(attributed_to);
CREATE INDEX IF NOT EXISTS idx_expenses_expense_date          ON expenses(expense_date);
