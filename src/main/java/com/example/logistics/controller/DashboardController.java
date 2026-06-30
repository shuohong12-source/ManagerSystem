package com.example.logistics.controller;

import com.example.logistics.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("overview", dashboardService.overview());
        model.addAttribute("regionVolumes", dashboardService.regionVolumes());
        model.addAttribute("topGoods", dashboardService.topGoods());
        model.addAttribute("lowInventory", dashboardService.lowInventory());
        model.addAttribute("statusDistribution", dashboardService.statusDistribution());
        model.addAttribute("monthlyTrend", dashboardService.monthlyTrend());
        return "index";
    }
}
