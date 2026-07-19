package com.zakpruitt.collectingwithzak.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class LotRequest {
    @NotBlank
    private String sellerName;
    @NotNull
    private LocalDate purchaseDate;
    @Positive
    private double totalCost;
    @PositiveOrZero
    private double estimatedMarketValue;
    private String description;
    @Valid
    @NotEmpty
    private List<SnapshotItem> items;
}
