package com.zakpruitt.collectingwithzak.ebay;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One card from the review form. Only the user-editable fields round-trip;
 * the card itself is re-derived server-side from {@code key} ("lotId:snapshotIndex").
 */
@Data
@NoArgsConstructor
public class StageListingRow {
    private String key;
    private String title;
    private double price;
    private Double compPrice;
}
