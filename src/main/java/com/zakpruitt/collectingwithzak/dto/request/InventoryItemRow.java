package com.zakpruitt.collectingwithzak.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InventoryItemRow {
    @NotBlank
    private String name;
    @NotBlank
    private String itemType;
    private double costBasis;
    private double marketValue;
    private String pokemonCardId;
    private String sealedProductId;
    private String setName;
    private String cardNumber;
    private String imageUrl;
    private String gradingCompany;
}
