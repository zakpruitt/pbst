package com.zakpruitt.collectingwithzak.repository;

import com.zakpruitt.collectingwithzak.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByOrderByExpenseDateDescIdDesc();
}
