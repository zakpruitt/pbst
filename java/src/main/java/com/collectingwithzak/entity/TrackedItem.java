package com.collectingwithzak.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "tracked_items")
@Getter
@Setter
public class TrackedItem extends BaseEntity {

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "cost_basis")
    private double costBasis;

    @Column(name = "market_value_at_purchase")
    private double marketValueAtPurchase;

    @Column(name = "manual_name_override")
    private String manualNameOverride;

    private String notes;

    private String purpose;

    @Column(name = "item_type")
    private String itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_purchase_id")
    private LotPurchase lotPurchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pokemon_card_id")
    private PokemonCard pokemonCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sealed_product_id")
    private SealedProduct sealedProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_submission_id")
    private GradingSubmission gradingSubmission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @Embedded
    private GradedDetails gradedDetails;

    public double getGradingFee() {
        if (gradingSubmission == null) {
            return 0;
        }
        return gradingSubmission.getCostPerCard();
    }

    public double getTotalCostBasis() {
        double total = costBasis + getGradingFee();
        if (gradedDetails != null) {
            total += gradedDetails.getGradingUpcharge();
        }
        return total;
    }
}
