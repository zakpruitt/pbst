UPDATE tracked_items ti
SET pokemon_card_id = sub.card_id
FROM (
    SELECT DISTINCT ON (ti2.id) ti2.id AS item_id, elem->>'pokemon_card_id' AS card_id
    FROM tracked_items ti2
    JOIN lot_purchases lp ON ti2.lot_purchase_id = lp.id
    CROSS JOIN LATERAL jsonb_array_elements(lp.lot_content_snapshot::jsonb) AS elem
    JOIN pokemon_cards pc ON pc.id = elem->>'pokemon_card_id'
    WHERE ti2.pokemon_card_id IS NULL
      AND ti2.deleted_at IS NULL
      AND lp.deleted_at IS NULL
      AND elem->>'pokemon_card_id' IS NOT NULL
      AND elem->>'pokemon_card_id' != ''
      AND elem->>'name' = ti2.manual_name_override
      AND (elem->>'is_tracked')::boolean = true
    ORDER BY ti2.id
) sub
WHERE ti.id = sub.item_id;
