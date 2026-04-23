package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.dto.response.ExpenseResponse;
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

    public void create(CreateExpenseRequest request) {
        Expense expense = expenseMapper.toEntity(request);
        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }
        expenseRepo.save(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAll() {
        return expenseMapper.toResponseList(expenseRepo.findAllByOrderByExpenseDateDescIdDesc());
    }

    public void delete(Long id) {
        expenseRepo.deleteById(id);
    }
}
