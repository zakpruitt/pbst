package com.zakpruitt.pbst.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleDTO {
    private Long id;
    private String ebayOrderId;
    private LocalDate saleDate;
    private String title;
    private String buyerUsername;
    private BigDecimal grossAmount;
    private BigDecimal ebayFees;
    private BigDecimal shippingCost;
    private BigDecimal netAmount;
    private String imageUrl;
    private String orderStatus;
    private String notes;
    private List<Long> trackedItemIds;
}
