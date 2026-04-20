package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.entity.Expense;
import com.collectingwithzak.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public String index(Model model) {
        List<Expense> expenses = expenseService.getAll();
        double total = expenses.stream().mapToDouble(Expense::getCost).sum();

        model.addAttribute("page", "expenses");
        model.addAttribute("groups", MonthGroup.groupByMonth(expenses, Expense::getExpenseDate));
        model.addAttribute("total", total);
        model.addAttribute("count", expenses.size());
        return "expenses/index";
    }

    @PostMapping
    public String create(CreateExpenseRequest request) {
        expenseService.create(request);
        return "redirect:/expenses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }

}
