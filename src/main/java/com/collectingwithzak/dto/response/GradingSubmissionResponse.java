package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradingSubmissionResponse {
    private Long id;
    private String submissionName;
    private String company;
    private String status;
    private String submissionMethod;
    private LocalDate sendDate;
    private LocalDate returnDate;
    private String notes;
    private double costPerCard;
    private double submissionCost;
    private double upchargeTotal;
    private double grandTotal;
    private LocalDateTime createdAt;
    private List<TrackedItemResponse> items;
}
