CREATE TABLE IF NOT EXISTS ebay_listings (
    id              BIGSERIAL PRIMARY KEY,
    lot_purchase_id BIGINT NOT NULL REFERENCES lot_purchases(id),
    snapshot_index  INT NOT NULL,
    card_name       TEXT NOT NULL DEFAULT '',
    set_name        TEXT NOT NULL DEFAULT '',
    card_number     TEXT NOT NULL DEFAULT '',
    image_url       TEXT NOT NULL DEFAULT '',
    qty             INT NOT NULL DEFAULT 1,
    market_price    NUMERIC(10,2) NOT NULL DEFAULT 0,
    comp_price      NUMERIC(10,2),
    listed_price    NUMERIC(10,2) NOT NULL DEFAULT 0,
    sku             TEXT NOT NULL DEFAULT '',
    offer_id        TEXT NOT NULL DEFAULT '',
    ebay_listing_id TEXT NOT NULL DEFAULT '',
    status          TEXT NOT NULL DEFAULT 'STAGED',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ebay_listings_lot_snapshot UNIQUE (lot_purchase_id, snapshot_index)
);

CREATE INDEX IF NOT EXISTS idx_ebay_listings_lot ON ebay_listings(lot_purchase_id);
