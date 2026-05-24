package com.zakpruitt.collectingwithzak.entity;

import com.zakpruitt.collectingwithzak.entity.enums.GradingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grading_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingSubmission extends BaseEntity {

    @Column(name = "submission_name")
    private String submissionName;

    private String company;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private GradingStatus status = GradingStatus.PREPPING;

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

    @Builder.Default
    @OneToMany(mappedBy = "gradingSubmission", fetch = FetchType.LAZY)
    private List<TrackedItem> items = new ArrayList<>();

    public double getGrandTotal() {
        return submissionCost + upchargeTotal;
    }
}
