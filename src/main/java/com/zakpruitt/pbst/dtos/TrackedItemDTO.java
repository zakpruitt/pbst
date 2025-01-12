package com.zakpruitt.pbst.dtos;

import com.zakpruitt.pbst.enums.GradingCompany;
import com.zakpruitt.pbst.enums.ItemGradingStatus;
import com.zakpruitt.pbst.enums.ItemType;
import com.zakpruitt.pbst.enums.Purpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackedItemDTO {
    private Long id;
    private Long lotPurchaseId;
    private String pokemonCardId; // ID from PokemonCard entity
    private String manualNameOverride;
    private ItemType itemType;
    private Purpose purpose;
    private ItemGradingStatus gradingStatus;
    
    // Grading info
    private Long gradingSubmissionId;
    private GradingCompany gradingCompany;
    private String grade;
    private BigDecimal gradingUpcharge;
    
    // Sale info
    private Long saleId;

    // Form fields
    private Boolean isTracked;
    private String name; // Maps to manualNameOverride or card name
    private String setName; // From PokemonCard
    private String imageUrl; // From PokemonCard
    private BigDecimal costBasis;
    private BigDecimal marketValueAtPurchase;
    private BigDecimal marketPrice; // Live value from PokemonCard
    
    private Integer quantity; // For creating multiple items
}
