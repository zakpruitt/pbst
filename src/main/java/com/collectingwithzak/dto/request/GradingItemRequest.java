package com.collectingwithzak.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class GradingItemRequest {
    @NotNull
    private Long itemId;
    private String grade;
    @PositiveOrZero
    private double upcharge;
}
