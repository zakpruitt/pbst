package com.collectingwithzak.controller;

import com.collectingwithzak.dto.expense.CreateExpenseRequest;
import com.collectingwithzak.dto.expense.ExpenseIndexData;
import com.collectingwithzak.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public String index(Model model) {
        ExpenseIndexData data = expenseService.getIndexData();
        model.addAttribute("data", data);
        return "expenses/index";
    }

    @GetMapping("/new")
    public String newExpense() {
        return "expenses/new";
    }

    @PostMapping
    public String create(@Valid CreateExpenseRequest request) {
        expenseService.create(request);
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }

}
