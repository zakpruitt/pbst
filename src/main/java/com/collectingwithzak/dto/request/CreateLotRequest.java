package com.collectingwithzak.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateLotRequest {
    private String sellerName;
    private LocalDate purchaseDate;
    private double totalCost;
    private double estimatedMarketValue;
    private String description;
    private String lotContentSnapshot;
}
