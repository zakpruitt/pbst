-- Backfill pokemon_card_id for tracked items that were created via inventory form
-- Match by manual_name_override against pokemon_cards.name, only when there's exactly one match
UPDATE tracked_items ti
SET pokemon_card_id = sub.card_id
FROM (
    SELECT ti2.id AS item_id, pc.id AS card_id
    FROM tracked_items ti2
    JOIN pokemon_cards pc ON pc.name = ti2.manual_name_override AND pc.deleted_at IS NULL
    WHERE ti2.pokemon_card_id IS NULL
      AND ti2.deleted_at IS NULL
      AND ti2.manual_name_override IS NOT NULL
      AND ti2.manual_name_override != ''
      AND ti2.item_type IN ('RAW_CARD', 'GRADED_CARD')
      AND ti2.id NOT IN (
          SELECT ti3.id
          FROM tracked_items ti3
          JOIN pokemon_cards pc2 ON pc2.name = ti3.manual_name_override AND pc2.deleted_at IS NULL
          WHERE ti3.pokemon_card_id IS NULL
            AND ti3.deleted_at IS NULL
            AND ti3.manual_name_override IS NOT NULL
            AND ti3.manual_name_override != ''
          GROUP BY ti3.id
          HAVING COUNT(pc2.id) > 1
      )
) sub
WHERE ti.id = sub.item_id;
