package com.collectingwithzak.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grading_submissions")
@Getter
@Setter
@SQLDelete(sql = "UPDATE grading_submissions SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class GradingSubmission extends BaseEntity {

    @Column(name = "submission_name")
    private String submissionName;

    private String company;

    private String status = "PREPPING";

    @Column(name = "submission_method")
    private String submissionMethod;

    @Column(name = "send_date")
    private LocalDate sendDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    private String notes;

    @Column(name = "cost_per_card", columnDefinition = "numeric(10,2)")
    private double costPerCard;

    @Column(name = "tax_rate", columnDefinition = "numeric(6,5)")
    private double taxRate;

    @Column(name = "submission_cost", columnDefinition = "numeric(10,2)")
    private double submissionCost;

    @Column(name = "upcharge_total", columnDefinition = "numeric(10,2)")
    private double upchargeTotal;

    @OneToMany(mappedBy = "gradingSubmission", fetch = FetchType.LAZY)
    private List<TrackedItem> items = new ArrayList<>();

    public double getGrandTotal() {
        return submissionCost + upchargeTotal;
    }
}
