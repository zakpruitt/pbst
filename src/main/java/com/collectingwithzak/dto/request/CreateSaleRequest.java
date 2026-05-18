package com.collectingwithzak.dto.request;

import com.collectingwithzak.entity.enums.Origin;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSaleRequest {
    private String ebayOrderId;
    private String title;
    private String buyerUsername;
    private LocalDate saleDate;
    private double grossAmount;
    private double ebayFees;
    private double shippingCost;
    private String origin = Origin.EBAY.name();
}
