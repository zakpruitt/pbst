package com.zakpruitt.collectingwithzak.service.render;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.render.ExpenseIndexData;
import com.zakpruitt.collectingwithzak.dto.response.ExpenseResponse;
import com.zakpruitt.collectingwithzak.mapper.ExpenseMapper;
import com.zakpruitt.collectingwithzak.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseRenderService {

    private final ExpenseRepository expenseRepo;
    private final ExpenseMapper expenseMapper;

    public ExpenseIndexData getIndexData() {
        List<ExpenseResponse> expenses = expenseMapper.toResponseList(expenseRepo.findAllByOrderByExpenseDateDescIdDesc());
        List<MonthGroup<ExpenseResponse>> groups = MonthGroup.groupByMonth(expenses,
                ExpenseResponse::getExpenseDate, ExpenseResponse::getCost);

        double total = expenses.stream().mapToDouble(ExpenseResponse::getCost).sum();

        return ExpenseIndexData.builder()
                .groups(groups)
                .total(total)
                .count(expenses.size())
                .avg(expenses.isEmpty() ? 0 : total / expenses.size())
                .total30(totalSince(expenses, LocalDate.now().minusDays(30)))
                .totalMonth(totalSince(expenses, LocalDate.now().withDayOfMonth(1)))
                .build();
    }

    private double totalSince(List<ExpenseResponse> expenses, LocalDate since) {
        return expenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(since))
                .mapToDouble(ExpenseResponse::getCost)
                .sum();
    }
}
