package com.campus.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.campus.management.service.DashboardService;
import com.campus.management.service.RegistrationService;
import com.campus.management.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // 系统首页
    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "1") int registrationPage,
                        Authentication authentication,
                        Model model) {
        model.addAttribute("systemName", "校园活动管理系统");
        // 获取热门活动数据
        model.addAttribute("hotActivities", dashboardService.getHotActivities());
        // 登录用户展示个人报名记录
        if (authentication != null && authentication.isAuthenticated()) {
            SysUser currentUser = userService.findByUsername(authentication.getName());
            Page<ActivityRegistration> registrationPageData = registrationService.pageMyRegistrations(currentUser.getId(), registrationPage, 5);
            model.addAttribute("registrationPage", registrationPageData);
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}


