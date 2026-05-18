package com.collectingwithzak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SnapshotItem {

    private String name;

    @JsonProperty("pokemon_card_id")
    private String pokemonCardId;

    @JsonProperty("set_name")
    private String setName;

    @JsonProperty("card_number")
    private String cardNumber;

    private String rarity;

    private int qty = 1;

    @JsonProperty("market_price")
    private double marketPrice;

    private double percentage;

    private double offered;

    @JsonProperty("item_type")
    private String itemType;

    private String purpose;

    @JsonProperty("grading_company")
    private String gradingCompany;

    private String grade;

    @JsonProperty("is_tracked")
    private boolean isTracked;

    @JsonProperty("image_url")
    private String imageUrl;
}
