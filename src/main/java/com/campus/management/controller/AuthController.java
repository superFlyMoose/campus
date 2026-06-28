package com.campus.management.controller;

import com.campus.management.dto.RegisterForm;
import com.campus.management.entity.SysUser;
import com.campus.management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // 注册页面
    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "register";
    }

    // 用户注册提交
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterForm registerForm,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        // 校验两次密码是否一致
        if (!bindingResult.hasFieldErrors("password")
                && !bindingResult.hasFieldErrors("confirmPassword")
                && !registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "registerError", "两次输入的密码不一致");
        }
        // 表单校验失败则返回注册页
        if (bindingResult.hasErrors()) {
            return "register";
        }
        // 构建用户实体并加密密码
        SysUser user = new SysUser();
        user.setUsername(registerForm.getUsername());
        user.setRealName(registerForm.getRealName());
        user.setPassword(passwordEncoder.encode(registerForm.getPassword()));
        try {
            userService.register(user);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("registerError", ex.getMessage());
            return "register";
        }
        redirectAttributes.addFlashAttribute("successMessage", "注册成功，请登录");
        return "redirect:/login";
    }
}
