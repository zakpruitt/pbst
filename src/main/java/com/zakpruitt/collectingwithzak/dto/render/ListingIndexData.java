package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.ListingCandidate;
import com.zakpruitt.collectingwithzak.entity.EbayListing;

import java.util.List;

public record ListingIndexData(
        List<ListingCandidate> candidates,
        List<EbayListing> listings,
        boolean ebayConfigured) {
}
