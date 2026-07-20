package com.zakpruitt.collectingwithzak.entity;

import com.zakpruitt.collectingwithzak.entity.enums.ItemStatus;
import com.zakpruitt.collectingwithzak.entity.enums.ItemType;
import com.zakpruitt.collectingwithzak.entity.enums.Purpose;
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

    @Column(name = "cost_basis", columnDefinition = "numeric(10,2)")
    private double costBasis;

    @Column(name = "market_value_at_purchase", columnDefinition = "numeric(10,2)")
    private double marketValueAtPurchase;

    @Column(name = "manual_name_override")
    private String manualNameOverride;

    private String notes;

    @Enumerated(EnumType.STRING)
    private Purpose purpose = Purpose.INVENTORY;

    @Enumerated(EnumType.STRING)
    private ItemStatus status = ItemStatus.AVAILABLE;

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

    public void attachTo(GradingSubmission submission) {
        this.gradingSubmission = submission;
        this.status = ItemStatus.IN_GRADING;
    }

    public void releaseFromGrading() {
        this.gradingSubmission = null;
        this.status = ItemStatus.AVAILABLE;
    }

    public void returnFromGrading(GradedDetails details) {
        this.gradedDetails = details;
        this.itemType = ItemType.GRADED_CARD;
        this.status = ItemStatus.AVAILABLE;
    }

    public String getGradingCompany() {
        return gradedDetails == null ? null : gradedDetails.getGradingCompany();
    }

    public String getGrade() {
        return gradedDetails == null ? null : gradedDetails.getGrade();
    }

    public double getGradingFee() {
        if (gradingSubmission == null) {
            return 0;
        }
        return gradingSubmission.getCostPerCard();
    }

    public void attachTo(Sale sale) {
        this.sale = sale;
        this.status = ItemStatus.SOLD;
    }

    public void releaseFromSale() {
        this.sale = null;
        this.status = ItemStatus.AVAILABLE;
    }

    public String getTypeLabel() {
        return itemType.getLabel();
    }

    public double getTotalCostBasis() {
        double total = costBasis + getGradingFee();
        if (gradedDetails != null) {
            total += gradedDetails.getGradingUpcharge();
        }
        return total;
    }

    public double getEffectiveMarketValue() {
        if (marketValueAtPurchase != 0) {
            return marketValueAtPurchase;
        }
        return pokemonCard != null ? pokemonCard.getMarketPrice() : 0;
    }
}
