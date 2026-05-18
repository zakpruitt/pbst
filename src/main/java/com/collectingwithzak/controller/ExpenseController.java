package com.collectingwithzak.controller;

import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.dto.response.ExpensePageData;
import com.collectingwithzak.service.ExpenseService;
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

    // ---------- Create ----------

    @GetMapping("/new")
    public String newExpense() {
        return "expenses/new";
    }

    @PostMapping
    public String create(CreateExpenseRequest request) {
        expenseService.create(request);
        return "redirect:/expenses";
    }

    // ---------- Read ----------

    @GetMapping
    public String index(Model model) {
        ExpensePageData data = expenseService.getPageData();

        model.addAttribute("groups", data.getGroups());
        model.addAttribute("total", data.getTotal());
        model.addAttribute("count", data.getCount());
        model.addAttribute("avg", data.getAvg());
        model.addAttribute("total30", data.getTotal30());
        model.addAttribute("count30", data.getCount30());
        model.addAttribute("totalMonth", data.getTotalMonth());
        model.addAttribute("countMonth", data.getCountMonth());
        return "expenses/index";
    }

    // ---------- Delete ----------

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        expenseService.delete(id);
        return "redirect:/expenses";
    }

}
