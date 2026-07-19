package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentType {
    PAYOUT("Payout"),
    RECEIVABLE("He Owes Me");

    private final String label;
}
