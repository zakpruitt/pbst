package com.zakpruitt.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EbayListingResponse {
    private Long id;
    private Long lotPurchaseId;
    private int snapshotIndex;
    private String cardName;
    private String setName;
    private String cardNumber;
    private String imageUrl;
    private int qty;
    private double marketPrice;
    private Double compPrice;
    private double listedPrice;
    private String sku;
    private String offerId;
    private String ebayListingId;
    private String status;
    private LocalDateTime createdAt;
}
