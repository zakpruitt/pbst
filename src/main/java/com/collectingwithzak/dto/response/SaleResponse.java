package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
    private Long id;
    private String ebayOrderId;
    private LocalDate saleDate;
    private String title;
    private String buyerUsername;
    private double grossAmount;
    private double ebayFees;
    private double shippingCost;
    private double netAmount;
    private String imageUrl;
    private String orderStatus;
    private String origin;
    private String status;
    private String attributedTo;
    private String notes;
    private List<TrackedItemResponse> items;
}
