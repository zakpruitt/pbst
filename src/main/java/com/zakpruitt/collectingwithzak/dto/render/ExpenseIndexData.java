package com.zakpruitt.collectingwithzak.dto.render;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.entity.Expense;

import java.util.List;

public record ExpenseIndexData(
        List<MonthGroup<Expense>> groups,
        double total,
        double avg,
        double total30,
        double totalMonth,
        int count) {
}
