package com.collectingwithzak.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateExpenseRequest {
    private String name;
    private LocalDate expenseDate;
    private double cost;
}
