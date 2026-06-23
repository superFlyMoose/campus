package com.campus.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.management.dto.UserForm;
import com.campus.management.entity.Activity;
import com.campus.management.entity.SysUser;
import com.campus.management.service.ActivityService;
import com.campus.management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {
    private final UserService userService;
    private final ActivityService activityService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserService userService,
                           ActivityService activityService,
                           PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.activityService = activityService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(@RequestParam(defaultValue = "activity") String tab,
                            @RequestParam(defaultValue = "1") int page,
                            Model model) {
        model.addAttribute("activeTab", tab);
        if ("users".equals(tab)) {
            Page<SysUser> userPage = userService.pageUsers(page, 10);
            model.addAttribute("userPage", userPage);
        } else {
            Page<Activity> activityPage = activityService.searchActivities("", "", page, 8);
            model.addAttribute("activityPage", activityPage);
            model.addAttribute("activeTab", "activity");
        }
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(defaultValue = "1") int page) {
        return "redirect:/admin/dashboard?tab=users&page=" + page;
    }

    @GetMapping("/admin/users/create")
    public String createUserPage(Model model) {
        if (!model.containsAttribute("userForm")) {
            UserForm userForm = new UserForm();
            userForm.setRole("USER");
            model.addAttribute("userForm", userForm);
        }
        model.addAttribute("pageTitle", "新增用户");
        model.addAttribute("formAction", "/admin/users/create");
        return "admin/user-form";
    }

    @PostMapping("/admin/users/create")
    public String createUser(@Valid @ModelAttribute UserForm userForm,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "新增用户");
            model.addAttribute("formAction", "/admin/users/create");
            return "admin/user-form";
        }
        try {
            SysUser user = new SysUser();
            user.setUsername(userForm.getUsername());
            user.setRealName(userForm.getRealName());
            user.setRole(userForm.getRole());
            user.setPassword(passwordEncoder.encode(userForm.getPassword()));
            userService.createUser(user);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("userError", ex.getMessage());
            model.addAttribute("pageTitle", "新增用户");
            model.addAttribute("formAction", "/admin/users/create");
            return "admin/user-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "用户创建成功");
        return "redirect:/admin/dashboard?tab=users";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editUserPage(@PathVariable Long id, Model model) {
        SysUser user = userService.getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        UserForm userForm = new UserForm();
        userForm.setUsername(user.getUsername());
        userForm.setRealName(user.getRealName());
        userForm.setRole(user.getRole());
        model.addAttribute("userForm", userForm);
        model.addAttribute("pageTitle", "编辑用户");
        model.addAttribute("formAction", "/admin/users/" + id + "/edit");
        return "admin/user-form";
    }

    @PostMapping("/admin/users/{id}/edit")
    public String editUser(@PathVariable Long id,
                           @Valid @ModelAttribute UserForm userForm,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "编辑用户");
            model.addAttribute("formAction", "/admin/users/" + id + "/edit");
            return "admin/user-form";
        }
        try {
            SysUser user = new SysUser();
            user.setUsername(userForm.getUsername());
            user.setRealName(userForm.getRealName());
            user.setRole(userForm.getRole());
            if (userForm.getPassword() != null && !userForm.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(userForm.getPassword()));
            }
            userService.updateUser(id, user);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("userError", ex.getMessage());
            model.addAttribute("pageTitle", "编辑用户");
            model.addAttribute("formAction", "/admin/users/" + id + "/edit");
            return "admin/user-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "用户更新成功");
        return "redirect:/admin/dashboard?tab=users";
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.removeById(id);
        redirectAttributes.addFlashAttribute("successMessage", "用户删除成功");
        return "redirect:/admin/dashboard?tab=users";
    }
}