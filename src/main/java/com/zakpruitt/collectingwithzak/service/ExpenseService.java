package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.common.MonthGroup;
import com.zakpruitt.collectingwithzak.dto.render.ExpenseIndexData;
import com.zakpruitt.collectingwithzak.dto.request.CreateExpenseRequest;
import com.zakpruitt.collectingwithzak.entity.Expense;
import com.zakpruitt.collectingwithzak.mapper.ExpenseMapper;
import com.zakpruitt.collectingwithzak.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final ExpenseMapper expenseMapper;

    @Transactional(readOnly = true)
    public ExpenseIndexData getIndexData() {
        List<Expense> expenses = expenseRepo.findAllByOrderByExpenseDateDescIdDesc();
        List<MonthGroup<Expense>> groups = MonthGroup.groupByMonth(expenses,
                Expense::getExpenseDate, Expense::getCost);

        double total = expenses.stream().mapToDouble(Expense::getCost).sum();

        return new ExpenseIndexData(groups, total,
                expenses.isEmpty() ? 0 : total / expenses.size(),
                totalSince(expenses, LocalDate.now().minusDays(30)),
                totalSince(expenses, LocalDate.now().withDayOfMonth(1)),
                expenses.size());
    }

    public void create(CreateExpenseRequest request) {
        expenseRepo.save(expenseMapper.toEntity(request));
    }

    public void delete(Long id) {
        expenseRepo.deleteById(id);
    }

    private double totalSince(List<Expense> expenses, LocalDate since) {
        return expenses.stream()
                       .filter(e -> !e.getExpenseDate().isBefore(since))
                       .mapToDouble(Expense::getCost)
                       .sum();
    }
}
