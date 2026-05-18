package com.collectingwithzak.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExpensePageData {
    private List<MonthGroup<ExpenseResponse>> groups;
    private double total;
    private int count;
    private double avg;
    private double total30;
    private int count30;
    private double totalMonth;
    private int countMonth;
}
