package com.collectingwithzak.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRowPreset {
    private String name;
    private String itemType;
    private double costBasis;
    private double marketPrice;
    private String pokemonCardId;
    private String sealedProductId;
    private String setName;
    private String cardNumber;
    private String imageUrl;
    private String gradingCompany;
}
