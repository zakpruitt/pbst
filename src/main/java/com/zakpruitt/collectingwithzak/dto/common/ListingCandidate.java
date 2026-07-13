package com.zakpruitt.collectingwithzak.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An untracked raw card from an accepted lot that could be listed on eBay.
 * Snapshot items have no ids of their own, so identity is the composite
 * {@code key} = "{lotPurchaseId}:{snapshotIndex}".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingCandidate {
    private Long lotPurchaseId;
    private int snapshotIndex;
    private String key;
    private String lotSellerName;
    private String name;
    private String setName;
    private String cardNumber;
    private String rarity;
    private int qty;
    private double marketPrice;
    private String imageUrl;
}
