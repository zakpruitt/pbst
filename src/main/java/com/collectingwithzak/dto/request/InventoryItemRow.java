package com.collectingwithzak.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InventoryItemRow {
    private String name = "";
    private String itemType = "";
    private double costBasis;
    private double marketValue;
    private String pokemonCardId = "";
    private String sealedProductId = "";
    private String setName = "";
    private String cardNumber = "";
    private String imageUrl = "";
    private String gradingCompany = "";
}
