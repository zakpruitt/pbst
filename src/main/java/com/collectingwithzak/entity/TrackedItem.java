package com.collectingwithzak.entity;

import com.collectingwithzak.entity.enums.ItemStatus;
import com.collectingwithzak.entity.enums.ItemType;
import com.collectingwithzak.entity.enums.Purpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tracked_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackedItem extends BaseEntity {

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "cost_basis", columnDefinition = "numeric(10,2)")
    private double costBasis;

    @Column(name = "market_value_at_purchase", columnDefinition = "numeric(10,2)")
    private double marketValueAtPurchase;

    @Column(name = "manual_name_override")
    private String manualNameOverride;

    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Purpose purpose = Purpose.INVENTORY;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.AVAILABLE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private ItemType itemType = ItemType.RAW_CARD;

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
