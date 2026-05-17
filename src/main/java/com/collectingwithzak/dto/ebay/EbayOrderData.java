package com.collectingwithzak.dto.ebay;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
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
