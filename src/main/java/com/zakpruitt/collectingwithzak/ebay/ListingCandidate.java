package com.zakpruitt.collectingwithzak.ebay;

import lombok.Builder;
import lombok.Data;

/**
 * An untracked raw card from an accepted lot that could be listed on eBay.
 * Snapshot items have no ids of their own, so identity is the composite
 * {@code key} = "{lotPurchaseId}:{snapshotIndex}".
 */
@Data
@Builder
public class ListingCandidate {
    private Long lotPurchaseId;
    private int snapshotIndex;
    private String lotSellerName;
    private String name;
    private String setName;
    private String cardNumber;
    private String rarity;
    private int qty;
    private double marketPrice;
    private String imageUrl;

    /**
     * The one spelling of the candidate key — exclusion sets and form round-trips must agree on it.
     */
    public static String key(Long lotPurchaseId, int snapshotIndex) {
        return lotPurchaseId + ":" + snapshotIndex;
    }

    public String getKey() {
        return key(lotPurchaseId, snapshotIndex);
    }
}
