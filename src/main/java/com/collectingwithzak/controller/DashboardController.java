package com.collectingwithzak.controller;

import com.collectingwithzak.dto.page.DashboardData;
import com.collectingwithzak.service.render.DashboardRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardRenderService dashboardRenderService;

    @GetMapping("/")
    public String renderDashboard(Model model) {
        DashboardData data = dashboardRenderService.getDashboardData();
        model.addAttribute("data", data);
        return "dashboard";
    }
}
