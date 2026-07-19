package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemType {
    RAW_CARD("Raw"),
    GRADED_CARD("Graded"),
    SEALED_PRODUCT("Sealed"),
    OTHER("Other");

    private final String label;
}
