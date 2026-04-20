package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.entity.Expense;
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

    public Expense create(CreateExpenseRequest request) {
        Expense expense = new Expense();
        expense.setName(request.getName());
        expense.setCost(request.getCost());
        expense.setExpenseDate(request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now());
        return expenseRepo.save(expense);
    }

    @Transactional(readOnly = true)
    public List<Expense> getAll() {
        return expenseRepo.findAllByOrderByExpenseDateDescIdDesc();
    }

    public void delete(Long id) {
        expenseRepo.deleteById(id);
    }
}
