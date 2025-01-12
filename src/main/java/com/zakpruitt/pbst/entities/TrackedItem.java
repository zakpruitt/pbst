package com.zakpruitt.pbst.entities;

import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.ItemType;
import com.zakpruitt.pbst.enums.Purpose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_purchase_id")
    private LotPurchase lotPurchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pokemon_card_id")
    private PokemonCard pokemonCard;

    // If it's sealed product or an unrecognized card, we can just type a name manually
    private String manualNameOverride;

    @Enumerated(EnumType.STRING)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    private ItemGradingStatus gradingStatus;

    private BigDecimal costBasis; // The calculated cost for this specific item
    private BigDecimal marketValueAtPurchase; // Snapshot of market value when bought

    // --- GRADING & SALES RELATIONS ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_submission_id")
    private GradingSubmission gradingSubmission;

    @Embedded
    private GradedDetails gradedDetails; // Holds slab info

    // If you eventually sell this specific graded card or PC item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;
}
