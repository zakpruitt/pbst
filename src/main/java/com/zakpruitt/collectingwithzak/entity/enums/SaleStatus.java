package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SaleStatus {
    STAGED("Staged"),
    CONFIRMED("Confirmed"),
    IGNORED("Ignored");

    private final String label;
}
