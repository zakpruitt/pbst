package com.zakpruitt.pbst.entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.zakpruitt.pbst.enums.GradingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
    private double estimatedSalePrice;

    @ManyToOne
    @JoinColumn(name = "sealed_product_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
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

