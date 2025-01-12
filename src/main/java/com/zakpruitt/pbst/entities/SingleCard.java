package com.zakpruitt.pbst.entities;

import com.zakpruitt.pbst.enums.GradingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SingleCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardName;
    private String setName;
    private boolean submittedForGrading;
    private double gradingCost;
    private LocalDate gradingSubmissionDate;
    private LocalDate gradingReturnDate;
    private String grade;
    private double estimatedSalePrice; // Pulled from external APIs

    @ManyToOne
    @JoinColumn(name = "sealed_product_id", nullable = false)
    private SealedProduct sealedProduct;

    public GradingStatus getGradingStatus() {
        if (!submittedForGrading) {
            return GradingStatus.UNGRADED;
        }
        if (submittedForGrading && gradingReturnDate == null) {
            return GradingStatus.IN_PROGRESS;
        }
        return GradingStatus.GRADING_COMPLETE;
    }
}
