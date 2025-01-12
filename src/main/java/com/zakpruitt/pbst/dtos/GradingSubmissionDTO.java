package com.zakpruitt.pbst.dtos;

import com.zakpruitt.pbst.enums.GradingCompany;
import com.zakpruitt.pbst.enums.SubmissionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingSubmissionDTO {
    private Long id;
    private String submissionName;
    private LocalDate sendDate;
    private LocalDate returnDate;
    private GradingCompany company;
    private SubmissionStatus status;
    private String submissionMethod;
    private String notes;
    private BigDecimal totalGradingCost;
    private List<TrackedItemDTO> items;
}
