package com.zakpruitt.pbst.entities;

import com.zakpruitt.pbst.enums.GradingCompany;
import com.zakpruitt.pbst.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String submissionName;
    private LocalDate sendDate;
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    private GradingCompany company;

    @Enumerated(EnumType.STRING)
    private SubmissionStatus status;

    private String submissionMethod; // e.g., "GameStop", "Self"

    @Column(columnDefinition = "TEXT")
    private String notes;

    private BigDecimal totalGradingCost;

    @OneToMany(mappedBy = "gradingSubmission")
    private List<TrackedItem> items = new ArrayList<>();
}
