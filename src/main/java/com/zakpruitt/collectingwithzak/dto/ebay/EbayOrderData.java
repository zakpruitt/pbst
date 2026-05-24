package com.zakpruitt.collectingwithzak.dto.ebay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EbayOrderData {
    private String ebayOrderId;
    private LocalDate saleDate;
    private String title;
    private String buyerUsername;
    private double grossAmount;
    private double ebayFees;
    private double shippingCost;
    private String orderStatus;
}
