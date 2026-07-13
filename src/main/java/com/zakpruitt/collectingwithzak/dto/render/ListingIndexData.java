package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.ListingCandidate;
import com.zakpruitt.collectingwithzak.dto.response.EbayListingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingIndexData {
    private List<ListingCandidate> candidates;
    private List<EbayListingResponse> listings;
    private boolean ebayConfigured;
}
