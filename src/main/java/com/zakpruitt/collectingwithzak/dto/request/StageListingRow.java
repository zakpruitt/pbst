package com.zakpruitt.collectingwithzak.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StageListingRow {
    private Long lotPurchaseId;
    private int snapshotIndex;
    private String name;
    private String setName;
    private String cardNumber;
    private String imageUrl;
    private int qty = 1;
    private double marketPrice;
    private Double compPrice;
    private double price;
    private String title;
}
