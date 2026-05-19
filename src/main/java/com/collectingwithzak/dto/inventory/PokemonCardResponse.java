package com.collectingwithzak.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonCardResponse {
    private String id;
    private String name;
    private String setCode;
    private String setName;
    private String cardNumber;
    private String rarity;
    private String imageUrl;
    private double marketPrice;
    private double lowPrice;
}
