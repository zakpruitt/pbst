package com.collectingwithzak.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grading_submissions")
@Getter
@Setter
public class GradingSubmission extends BaseEntity {

    @Column(name = "submission_name")
    private String submissionName;

    private String company;

    private String status;

    @Column(name = "submission_method")
    private String submissionMethod;

    @Column(name = "send_date")
    private LocalDate sendDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    private String notes;

    @Column(name = "cost_per_card")
    private double costPerCard;

    @Column(name = "tax_rate")
    private double taxRate;

    @Column(name = "submission_cost")
    private double submissionCost;

    @Column(name = "upcharge_total")
    private double upchargeTotal;

    @OneToMany(mappedBy = "gradingSubmission", fetch = FetchType.LAZY)
    private List<TrackedItem> items = new ArrayList<>();

    public double getGrandTotal() {
        return submissionCost + upchargeTotal;
    }
}
