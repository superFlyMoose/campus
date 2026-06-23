package com.campus.management.controller;

import com.campus.management.entity.SysUser;
import com.campus.management.service.RegistrationService;
import com.campus.management.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final UserService userService;

    public RegistrationController(RegistrationService registrationService, UserService userService) {
        this.registrationService = registrationService;
        this.userService = userService;
    }

    @PostMapping("/activity/{activityId}")
    public String register(@PathVariable Long activityId,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        try {
            registrationService.register(activityId, getCurrentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "报名成功");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/activities/" + activityId;
    }

    @PostMapping("/{registrationId}/cancel")
    public String cancel(@PathVariable Long registrationId,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            registrationService.cancel(registrationId, getCurrentUser(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "取消报名成功");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile";
    }

    private SysUser getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }
}
