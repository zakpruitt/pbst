package com.zakpruitt.collectingwithzak.ebay;

import java.util.List;

public record ListingIndexData(
        List<ListingCandidate> candidates,
        List<EbayListing> listings,
        boolean ebayConfigured) {
}
