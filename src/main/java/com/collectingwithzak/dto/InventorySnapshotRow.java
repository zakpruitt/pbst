package com.collectingwithzak.dto;

import com.collectingwithzak.entity.enums.ItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InventorySnapshotRow {

    private String name;

    @JsonProperty("cost_basis")
    private Double costBasis;

    @JsonProperty("market_value")
    private Double marketValue;

    @JsonProperty("pokemon_card_id")
    private String pokemonCardId;

    @JsonProperty("sealed_product_id")
    private String sealedProductId;

    @JsonProperty("item_type")
    private String itemType = ItemType.RAW_CARD.name();
}
