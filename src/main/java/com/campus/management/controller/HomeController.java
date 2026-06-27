package com.campus.management.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("systemName", "校园活动管理系统");
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
