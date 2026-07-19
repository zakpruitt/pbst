package com.zakpruitt.collectingwithzak.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class GradedDetails {

    @Column(name = "grading_company")
    private String gradingCompany;

    private String grade;

    @Column(name = "grading_upcharge", columnDefinition = "numeric(10,2)")
    private double gradingUpcharge;
}
