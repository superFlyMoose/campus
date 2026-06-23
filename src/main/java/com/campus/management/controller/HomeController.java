package com.campus.management.controller;

import com.campus.management.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final DashboardService dashboardService;

    public HomeController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("systemName", "校园活动管理系统");
        model.addAttribute("summary", dashboardService.getSummary());
        model.addAttribute("hotActivities", dashboardService.getHotActivities());
        model.addAttribute("chartData", dashboardService.getChartData());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
