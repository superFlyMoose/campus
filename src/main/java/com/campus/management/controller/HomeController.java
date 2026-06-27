package com.campus.management.controller;

import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.campus.management.service.DashboardService;
import com.campus.management.service.RegistrationService;
import com.campus.management.service.UserService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardService dashboardService;
    private final UserService userService;
    private final RegistrationService registrationService;

    public HomeController(DashboardService dashboardService,
                          UserService userService,
                          RegistrationService registrationService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        model.addAttribute("systemName", "校园活动管理系统");
        model.addAttribute("hotActivities", dashboardService.getHotActivities());
        if (authentication != null && authentication.isAuthenticated()) {
            SysUser currentUser = userService.findByUsername(authentication.getName());
            List<ActivityRegistration> registrations = registrationService.getMyRegistrations(currentUser.getId());
            model.addAttribute("registrations", registrations);
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

