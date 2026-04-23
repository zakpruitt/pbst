package com.collectingwithzak.dto.response;

import com.collectingwithzak.entity.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

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
    private String itemType;
    private double gradingFee;
    private double totalCostBasis;
    private PokemonCardResponse pokemonCard;
    private SealedProductResponse sealedProduct;
    private GradedDetailsResponse gradedDetails;
    private LotPurchaseSummary lotPurchase;
    private GradingSubmissionSummary gradingSubmission;

    public static List<TrackedItemResponse> filterByType(List<TrackedItemResponse> items, ItemType type) {
        return items.stream()
                .filter(i -> type.name().equals(i.getItemType()))
                .toList();
    }

    public static double sumCost(List<TrackedItemResponse> items) {
        return items.stream().mapToDouble(TrackedItemResponse::getTotalCostBasis).sum();
    }

    public static double sumMarket(List<TrackedItemResponse> items) {
        return items.stream().mapToDouble(TrackedItemResponse::getMarketValueAtPurchase).sum();
    }
}
