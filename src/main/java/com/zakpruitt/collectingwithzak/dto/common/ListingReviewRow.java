package com.zakpruitt.collectingwithzak.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One card on the review page: the candidate plus its comp research and suggested price. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingReviewRow {
    private ListingCandidate candidate;
    private Double compPrice;
    private String compTitle;
    private String compUrl;
    private int compCount;
    private double suggestedPrice;
    private String title;
}
