package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySplitResponse {
    private List<TrackedItemResponse> allItems;
    private List<TrackedItemResponse> rawItems;
    private List<TrackedItemResponse> gradedItems;
    private List<TrackedItemResponse> sealedItems;
    private List<TrackedItemResponse> otherItems;
}
