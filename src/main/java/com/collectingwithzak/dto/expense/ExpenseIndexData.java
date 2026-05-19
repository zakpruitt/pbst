package com.collectingwithzak.dto.expense;

import com.collectingwithzak.dto.common.MonthGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseIndexData {
    private List<MonthGroup<ExpenseResponse>> groups;
    private double total;
    private double avg;
    private double total30;
    private double totalMonth;
    private int count;
    private int count30;
    private int countMonth;
}
