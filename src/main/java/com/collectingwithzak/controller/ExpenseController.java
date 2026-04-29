package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.dto.response.ExpenseResponse;
import com.collectingwithzak.dto.response.MonthGroup;
import com.collectingwithzak.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public String index(Model model) {
        List<ExpenseResponse> expenses = expenseService.getAll();
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

        model.addAttribute("groups", groups);
        model.addAttribute("total", total);
        model.addAttribute("count", expenses.size());
        model.addAttribute("avg", avg);
        model.addAttribute("total30", last30.stream().mapToDouble(ExpenseResponse::getCost).sum());
        model.addAttribute("count30", last30.size());
        model.addAttribute("totalMonth", thisMonth.stream().mapToDouble(ExpenseResponse::getCost).sum());
        model.addAttribute("countMonth", thisMonth.size());
        return "expenses/index";
    }

    @GetMapping("/new")
    public String newExpense() {
        return "expenses/new";
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
