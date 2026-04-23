package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradingSubmissionSummary {
    private Long id;
    private String submissionName;
    private double costPerCard;
}
