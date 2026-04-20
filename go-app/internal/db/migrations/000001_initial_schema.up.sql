CREATE TABLE pokemon_cards (
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

CREATE TABLE sealed_products (
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

CREATE TABLE lot_purchases (
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

CREATE TABLE grading_submissions (
    id                BIGSERIAL PRIMARY KEY,
    submission_name   TEXT NOT NULL DEFAULT '',
    send_date         TIMESTAMPTZ,
    return_date       TIMESTAMPTZ,
    company           TEXT NOT NULL DEFAULT '',
    status            TEXT NOT NULL DEFAULT 'PREPPING',
    submission_method TEXT NOT NULL DEFAULT '',
    notes             TEXT,
    total_grading_cost NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);

CREATE TABLE tracked_items (
    id                      BIGSERIAL PRIMARY KEY,
    acquisition_date        TIMESTAMPTZ,
    cost_basis              NUMERIC(10,2) NOT NULL DEFAULT 0,
    market_value_at_purchase NUMERIC(10,2) NOT NULL DEFAULT 0,
    manual_name_override    TEXT,
    notes                   TEXT,
    purpose                 TEXT NOT NULL DEFAULT 'PERSONAL_COLLECTION',
    lot_purchase_id         BIGINT NOT NULL REFERENCES lot_purchases(id),
    pokemon_card_id         TEXT REFERENCES pokemon_cards(id),
    sealed_product_id       TEXT REFERENCES sealed_products(id),
    grading_submission_id   BIGINT REFERENCES grading_submissions(id),
    grading_company         TEXT NOT NULL DEFAULT '',
    grade                   TEXT NOT NULL DEFAULT '',
    grading_upcharge        NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at              TIMESTAMPTZ,
    CONSTRAINT chk_item_reference CHECK (
        (pokemon_card_id IS NOT NULL)::int +
        (sealed_product_id IS NOT NULL)::int +
        (manual_name_override IS NOT NULL AND manual_name_override <> '')::int = 1
    )
);

CREATE TABLE sales (
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
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at     TIMESTAMPTZ
);

-- soft delete indexes (GORM queries these on every read)
CREATE INDEX idx_lot_purchases_deleted_at       ON lot_purchases(deleted_at);
CREATE INDEX idx_tracked_items_deleted_at       ON tracked_items(deleted_at);
CREATE INDEX idx_grading_submissions_deleted_at ON grading_submissions(deleted_at);
CREATE INDEX idx_sales_deleted_at               ON sales(deleted_at);
CREATE INDEX idx_pokemon_cards_deleted_at       ON pokemon_cards(deleted_at);
CREATE INDEX idx_sealed_products_deleted_at     ON sealed_products(deleted_at);

-- common query indexes
CREATE INDEX idx_tracked_items_purpose         ON tracked_items(purpose);
CREATE INDEX idx_tracked_items_lot_purchase_id ON tracked_items(lot_purchase_id);
CREATE INDEX idx_pokemon_cards_name            ON pokemon_cards(name);
