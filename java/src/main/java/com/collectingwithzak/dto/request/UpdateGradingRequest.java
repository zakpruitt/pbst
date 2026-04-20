package com.collectingwithzak.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateGradingRequest {
    private String company;
    private String submissionMethod;
    private double submissionCost;
    private String notes;
    private List<Long> itemIds;
}
