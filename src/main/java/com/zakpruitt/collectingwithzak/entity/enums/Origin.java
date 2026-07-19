package com.zakpruitt.collectingwithzak.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Origin {
    EBAY("eBay"),
    FACEBOOK("Facebook"),
    OTHER("Other");

    private final String label;
}
