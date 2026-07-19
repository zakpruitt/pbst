package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Purpose {
    INVENTORY("Inventory"),
    PERSONAL_COLLECTION("Personal");

    private final String label;
}
