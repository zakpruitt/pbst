package com.zakpruitt.collectingwithzak.dto.request;

import com.zakpruitt.collectingwithzak.entity.enums.Origin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSaleRequest {
    private String ebayOrderId;
    @NotBlank
    private String title;
    private String buyerUsername;
    @NotNull
    private LocalDate saleDate;
    @Positive
    private double grossAmount;
    @PositiveOrZero
    private double ebayFees;
    @PositiveOrZero
    private double shippingCost;
    @NotBlank
    private String origin = Origin.EBAY.name();
}
