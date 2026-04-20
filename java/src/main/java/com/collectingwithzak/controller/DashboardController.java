package com.collectingwithzak.controller;

import com.collectingwithzak.dto.response.DashboardData;
import com.collectingwithzak.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String dashboard(Model model) {
        DashboardData data = dashboardService.getDashboardData();
        model.addAttribute("data", data);
        model.addAttribute("page", "dashboard");
        return "dashboard";
    }
}
