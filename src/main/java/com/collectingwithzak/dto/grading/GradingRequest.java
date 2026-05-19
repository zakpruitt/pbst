package com.collectingwithzak.dto.grading;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class GradingRequest {
    @NotBlank
    private String company;
    private String submissionMethod;
    @PositiveOrZero
    private double submissionCost;
    private String notes;
    private List<Long> itemIds = new ArrayList<>();
    private List<GradingItemRequest> grades = new ArrayList<>();
}
