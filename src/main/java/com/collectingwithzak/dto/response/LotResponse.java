package com.collectingwithzak.dto.response;

import com.collectingwithzak.dto.SnapshotItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LotResponse {
    private Long id;
    private String sellerName;
    private String description;
    private LocalDate purchaseDate;
    private double totalCost;
    private double estimatedMarketValue;
    private String status;
    private LocalDateTime createdAt;
    private List<TrackedItemResponse> trackedItems;
    private List<SnapshotItem> snapshotItems;
}
