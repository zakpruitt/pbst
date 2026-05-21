package com.collectingwithzak.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateInventoryRequest {
    private String name;
    private double costBasis;
    private double marketValue;
    private LocalDate acquisitionDate;
    private String notes;
    private String purpose;
    private String gradingCompany;
    private String grade;
}
