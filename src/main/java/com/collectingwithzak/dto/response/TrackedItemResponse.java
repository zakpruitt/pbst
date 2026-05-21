package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackedItemResponse {
    private Long id;
    private LocalDate acquisitionDate;
    private double costBasis;
    private double marketValueAtPurchase;
    private String manualNameOverride;
    private String notes;
    private String purpose;
    private String status;
    private String itemType;
    private double gradingFee;
    private double totalCostBasis;
    private PokemonCardResponse pokemonCard;
    private SealedProductResponse sealedProduct;

    private String gradingCompany;
    private String grade;
    private double gradingUpcharge;

    private Long lotPurchaseId;
    private String lotPurchaseSellerName;

    private Long gradingSubmissionId;
    private String gradingSubmissionName;
    private double gradingCostPerCard;
}
