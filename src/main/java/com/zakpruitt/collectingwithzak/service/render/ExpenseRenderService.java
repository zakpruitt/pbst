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
        List<ExpenseResponse> expenses = getAll();
        List<MonthGroup<ExpenseResponse>> groups = MonthGroup.groupByMonth(expenses,
                ExpenseResponse::getExpenseDate, ExpenseResponse::getCost);

        double total = expenses.stream().mapToDouble(ExpenseResponse::getCost).sum();
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        List<ExpenseResponse> last30 = expenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(thirtyDaysAgo)).toList();
        List<ExpenseResponse> thisMonth = expenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(firstOfMonth)).toList();

        return ExpenseIndexData.builder()
                .groups(groups)
                .total(total)
                .count(expenses.size())
                .avg(expenses.isEmpty() ? 0 : total / expenses.size())
                .total30(last30.stream().mapToDouble(ExpenseResponse::getCost).sum())
                .count30(last30.size())
                .totalMonth(thisMonth.stream().mapToDouble(ExpenseResponse::getCost).sum())
                .countMonth(thisMonth.size())
                .build();
    }

    public List<ExpenseResponse> getAll() {
        return expenseMapper.toResponseList(expenseRepo.findAllByOrderByExpenseDateDescIdDesc());
    }
}
