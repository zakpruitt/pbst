package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.dto.response.ExpensePageData;
import com.collectingwithzak.dto.response.ExpenseResponse;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.entity.Expense;
import com.collectingwithzak.mapper.ExpenseMapper;
import com.collectingwithzak.repository.ExpenseRepository;
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

    // ---------- Create ----------

    public void create(CreateExpenseRequest request) {
        Expense expense = expenseMapper.toEntity(request);
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        expenseRepo.save(expense);
    }

    // ---------- Read ----------

    public ExpensePageData getPageData() {
        List<ExpenseResponse> expenses = expenseMapper.toResponseList(expenseRepo.findAllByOrderByExpenseDateDescIdDesc());
        List<MonthGroup<ExpenseResponse>> groups = MonthGroup.groupByMonth(expenses, ExpenseResponse::getExpenseDate);
        MonthGroup.computeSubtotals(groups, ExpenseResponse::getCost);

        double total = expenses.stream().mapToDouble(ExpenseResponse::getCost).sum();
        double avg = expenses.isEmpty() ? 0 : total / expenses.size();

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);

        List<ExpenseResponse> last30 = expenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(thirtyDaysAgo))
                .toList();
        List<ExpenseResponse> thisMonth = expenses.stream()
                .filter(e -> !e.getExpenseDate().isBefore(firstOfMonth))
                .toList();

        return new ExpensePageData(groups, total, expenses.size(), avg,
                last30.stream().mapToDouble(ExpenseResponse::getCost).sum(), last30.size(),
                thisMonth.stream().mapToDouble(ExpenseResponse::getCost).sum(), thisMonth.size());
    }

    // ---------- Delete ----------

    public void delete(Long id) {
        expenseRepo.deleteById(id);
    }
}
