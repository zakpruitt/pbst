package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.response.TrackedItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryIndexData {
    private List<TrackedItemResponse> items;
    private List<TrackedItemResponse> rawItems;
    private List<TrackedItemResponse> gradedItems;
    private List<TrackedItemResponse> sealedItems;
    private List<TrackedItemResponse> otherItems;
    private String purpose;
    private double totalCost;
    private double totalMarket;
}
