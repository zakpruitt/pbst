package com.zakpruitt.collectingwithzak.service;

import com.zakpruitt.collectingwithzak.dto.request.CreateExpenseRequest;
import com.zakpruitt.collectingwithzak.mapper.ExpenseMapper;
import com.zakpruitt.collectingwithzak.repository.ExpenseRepository;
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
