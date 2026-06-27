package com.campus.management.controller;

import com.campus.management.dto.ProfileCacheData;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.campus.management.service.ProfileCacheService;
import com.campus.management.service.RegistrationService;
import com.campus.management.service.UserService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController {
    private final UserService userService;
    private final RegistrationService registrationService;
    private final ProfileCacheService profileCacheService;

    public UserController(UserService userService, RegistrationService registrationService, ProfileCacheService profileCacheService) {
        this.userService = userService;
        this.registrationService = registrationService;
        this.profileCacheService = profileCacheService;
    }

    @GetMapping("/user/profile")
    public String profile(Authentication authentication, Model model) {
        SysUser currentUser = userService.findByUsername(authentication.getName());
        ProfileCacheData cachedProfile = profileCacheService.getProfile(currentUser.getId());
        if (cachedProfile != null) {
            model.addAttribute("currentUser", toCurrentUser(cachedProfile));
            model.addAttribute("registrations", toRegistrations(cachedProfile));
            model.addAttribute("dailyRegistrationChart", cachedProfile.getDailyRegistrationChart());
            return "user/profile";
        }

        List<ActivityRegistration> registrations = registrationService.getMyRegistrations(currentUser.getId());
        profileCacheService.cacheProfile(currentUser, registrations);
        ProfileCacheData profileData = profileCacheService.getProfile(currentUser.getId());
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("registrations", registrations);
        model.addAttribute("dailyRegistrationChart", profileData != null ? profileData.getDailyRegistrationChart() : null);
        return "user/profile";
    }

    private SysUser toCurrentUser(ProfileCacheData cachedProfile) {
        SysUser user = new SysUser();
        user.setId(cachedProfile.getUserId());
        user.setUsername(cachedProfile.getUsername());
        user.setRealName(cachedProfile.getRealName());
        user.setRole(cachedProfile.getRole());
        user.setAvatar(cachedProfile.getAvatar());
        user.setIsDeleted(0);
        return user;
    }

    private List<ActivityRegistration> toRegistrations(ProfileCacheData cachedProfile) {
        if (cachedProfile.getRegistrations() == null || cachedProfile.getRegistrations().isEmpty()) {
            return List.of();
        }
        return cachedProfile.getRegistrations().stream()
            .map(item -> {
                ActivityRegistration registration = new ActivityRegistration();
                registration.setId(item.getId());
                registration.setActivityId(item.getActivityId());
                registration.setUserId(item.getUserId());
                registration.setRegistrationTime(item.getRegistrationTime());
                registration.setIsDeleted(item.getIsDeleted());
                return registration;
            })
            .toList();
    }
}

