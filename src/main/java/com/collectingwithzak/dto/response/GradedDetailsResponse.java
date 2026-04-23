package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradedDetailsResponse {
    private String gradingCompany;
    private String grade;
    private double gradingUpcharge;
}
