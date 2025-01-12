package com.zakpruitt.pbst.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SingleCardDTO {

    private Long id;

    @NotBlank(message = "Card name must not be blank")
    private String cardName;

    @NotBlank(message = "Set name must not be blank")
    private String setName;

    private boolean submittedForGrading;

    @PositiveOrZero(message = "Grading cost must be zero or positive")
    private double gradingCost;

    private LocalDate gradingSubmissionDate;

    private LocalDate gradingReturnDate;

    @Size(max = 10, message = "Grade must be at most 10 characters")
    private String grade;

    @PositiveOrZero(message = "Estimated sale price must be zero or positive")
    private double estimatedSalePrice;

    @NotNull(message = "Sealed product ID must not be null")
    private Long sealedProductId;
}