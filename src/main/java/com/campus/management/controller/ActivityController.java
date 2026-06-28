package com.campus.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.management.dto.ActivityForm;
import com.campus.management.entity.Activity;
import com.campus.management.entity.SysUser;
import com.campus.management.service.ActivityCacheService;
import com.campus.management.service.ActivityService;
import com.campus.management.service.RegistrationService;
import com.campus.management.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/activities")
public class ActivityController {
    private final ActivityService activityService;
    private final ActivityCacheService activityCacheService;
    private final RegistrationService registrationService;
    private final UserService userService;

    public ActivityController(ActivityService activityService,
                              ActivityCacheService activityCacheService,
                              RegistrationService registrationService,
                              UserService userService) {
        this.activityService = activityService;
        this.activityCacheService = activityCacheService;
        this.registrationService = registrationService;
        this.userService = userService;
    }

    // 活动列表页，支持分页+搜索
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "") String location,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        Page<Activity> pageData = activityCacheService.getActivityList(keyword, location, page); // 先查缓存
        // 缓存未命中则查数据库并回填缓存
        if (pageData == null) {
            pageData = activityService.searchActivities(keyword, location, page, 6);
            activityCacheService.cacheActivityList(keyword, location, page, pageData);
        }
        model.addAttribute("pageData", pageData);
        model.addAttribute("keyword", keyword);
        model.addAttribute("location", location);
        return "activity/list";
    }

    // 活动详情页
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        Activity activity = activityCacheService.getActivityDetail(id); // 详情缓存查询
        if (activity == null) {
            activity = activityService.getActivityOrThrow(id);
            activityCacheService.cacheActivityDetail(activity);
        }
        model.addAttribute("activity", activity);
        model.addAttribute("canRegister", activityService.canRegister(activity));
        // 登录用户报名状态判断
        if (authentication != null) {
            SysUser currentUser = userService.findByUsername(authentication.getName());
            model.addAttribute("hasRegistered", registrationService.hasRegistered(id, currentUser.getId()));
        } else {
            model.addAttribute("hasRegistered", false);
        }
        return "activity/detail";
    }

    // 管理员：创建活动页面
    @GetMapping("/admin/create")
    public String createPage(Model model) {
        if (!model.containsAttribute("activityForm")) {
            model.addAttribute("activityForm", new ActivityForm());
        }
        model.addAttribute("formAction", "/activities/admin/create");
        model.addAttribute("pageTitle", "发布活动");
        return "activity/form";
    }

    // 管理员：创建活动提交
    @PostMapping("/admin/create")
    public String create(@Valid @ModelAttribute ActivityForm activityForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes,
                         @RequestParam(name = "imageFile", required = false) MultipartFile imageFile) {
        // 表单校验失败
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/activities/admin/create");
            model.addAttribute("pageTitle", "发布活动");
            return "activity/form";
        }
        try {
            activityService.bindActivityImage(activityForm, imageFile);
            activityService.createActivity(activityForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("activityError", ex.getMessage());
            model.addAttribute("formAction", "/activities/admin/create");
            model.addAttribute("pageTitle", "发布活动");
            return "activity/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "活动发布成功");
        return "redirect:/activities";
    }

    // 管理员：编辑页面
    @GetMapping("/admin/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        Activity activity = activityService.getActivityOrThrow(id);
        ActivityForm form = new ActivityForm();
        form.setTitle(activity.getTitle());
        form.setContent(activity.getContent());
        form.setLocation(activity.getLocation());
        form.setStartTime(activity.getStartTime());
        form.setEndTime(activity.getEndTime());
        form.setMaxPeople(activity.getMaxPeople());
        form.setImagePath(activity.getImagePath());
        model.addAttribute("activityForm", form);
        model.addAttribute("formAction", "/activities/admin/" + id + "/edit");
        model.addAttribute("pageTitle", "编辑活动");
        return "activity/form";
    }

    // 管理员：编辑提交
    @PostMapping("/admin/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute ActivityForm activityForm,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes,
                       @RequestParam(name = "imageFile", required = false) MultipartFile imageFile) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/activities/admin/" + id + "/edit");
            model.addAttribute("pageTitle", "编辑活动");
            return "activity/form";
        }
        try {
            activityService.bindActivityImage(activityForm, imageFile);
            activityService.updateActivity(id, activityForm);
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("activityError", ex.getMessage());
            model.addAttribute("formAction", "/activities/admin/" + id + "/edit");
            model.addAttribute("pageTitle", "编辑活动");
            return "activity/form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "活动更新成功");
        return "redirect:/activities/" + id;
    }

    // 管理员：删除活动
    @PostMapping("/admin/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        activityService.removeActivity(id);
        redirectAttributes.addFlashAttribute("successMessage", "活动删除成功");
        return "redirect:/activities";
    }
}
