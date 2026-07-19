package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradingStatus {
    PREPPING("Prepping"),
    IN_GRADING("In Grading"),
    ACCEPTED("Accepted"),
    RETURNED("Returned"),
    REJECTED("Rejected"),
    PENDING("Pending");

    private final String label;
}
