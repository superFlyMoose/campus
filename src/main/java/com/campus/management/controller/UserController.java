package com.campus.management.controller;

import com.campus.management.entity.SysUser;
import com.campus.management.service.RegistrationService;
import com.campus.management.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {

    private final UserService userService;
    private final RegistrationService registrationService;

    public UserController(UserService userService, RegistrationService registrationService) {
        this.userService = userService;
        this.registrationService = registrationService;
    }

    @GetMapping("/user/profile")
    public String profile(Authentication authentication, Model model) {
        SysUser currentUser = userService.findByUsername(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("registrations", registrationService.getMyRegistrations(currentUser.getId()));
        return "user/profile";
    }
}
