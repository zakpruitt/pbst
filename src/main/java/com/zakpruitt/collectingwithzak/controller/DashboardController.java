package com.zakpruitt.collectingwithzak.controller;

import com.zakpruitt.collectingwithzak.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String renderDashboard(Model model) {
        model.addAttribute("data", dashboardService.getDashboardData());
        return "dashboard";
    }
}
