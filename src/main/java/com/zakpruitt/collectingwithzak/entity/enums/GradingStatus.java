package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GradingStatus {
    PREPPING("Prepping"),
    IN_GRADING("In Grading"),
    RETURNED("Returned");

    private final String label;
}
