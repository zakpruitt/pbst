package com.zakpruitt.collectingwithzak.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateExpenseRequest {
    @NotBlank
    private String name;
    @NotNull
    private LocalDate expenseDate = LocalDate.now();
    @Positive
    private double cost;
}
