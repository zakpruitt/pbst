package com.zakpruitt.pbst.entities;

import com.zakpruitt.pbst.enums.GradingCompany;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradedDetails {
    @Enumerated(EnumType.STRING)
    private GradingCompany gradingCompany;

    private String grade;
    
    private BigDecimal gradingUpcharge;
}
