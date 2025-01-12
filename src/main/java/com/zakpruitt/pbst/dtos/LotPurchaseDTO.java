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
public class LotPurchaseDTO {
    private Long id;
    private LocalDate purchaseDate;
    private String description;
    private BigDecimal totalCost;
    private BigDecimal estimatedMarketValue;
    private String lotContentSnapshot;
    private String status;
    private List<TrackedItemDTO> trackedItems;
}
