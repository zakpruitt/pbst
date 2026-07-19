package com.zakpruitt.collectingwithzak.ebay;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * The review form: one row per card to stage, form-bound via indexed properties (rows[0].price).
 */
@Data
@NoArgsConstructor
public class StageListingsRequest {
    private List<StageListingRow> rows = new ArrayList<>();
}
