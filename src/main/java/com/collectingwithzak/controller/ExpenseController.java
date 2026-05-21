package com.collectingwithzak.controller;

import com.collectingwithzak.dto.page.ExpenseIndexData;
import com.collectingwithzak.dto.request.CreateExpenseRequest;
import com.collectingwithzak.service.ExpenseService;
import com.collectingwithzak.service.render.ExpenseRenderService;
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

    private final ExpenseRenderService expenseRenderService;
    private final ExpenseService expenseService;

    @GetMapping
    public String renderIndex(Model model) {
        ExpenseIndexData data = expenseRenderService.getIndexData();
        model.addAttribute("data", data);
        return "expenses/index";
    }

    @GetMapping("/new")
    public String renderNewForm() {
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
