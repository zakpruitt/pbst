package com.collectingwithzak.service;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.mapper.ExpenseMapper;
import com.collectingwithzak.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepo;
    private final ExpenseMapper expenseMapper;

    public void create(CreateExpenseRequest request) {
        expenseRepo.save(expenseMapper.toEntity(request));
    }

    public void delete(Long id) {
        expenseRepo.deleteById(id);
    }
}
