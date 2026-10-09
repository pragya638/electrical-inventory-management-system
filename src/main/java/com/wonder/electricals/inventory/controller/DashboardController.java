package com.wonder.electricals.inventory.controller;

import com.wonder.electricals.inventory.dto.response.DashboardSummary;
import com.wonder.electricals.inventory.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Landing page with the counter cards and the recent-activity tables. */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public String dashboard(Model model) {
        DashboardSummary summary = dashboardService.buildSummary();
        model.addAttribute("summary", summary);
        return "dashboard";
    }
}
