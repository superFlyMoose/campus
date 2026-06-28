package com.campus.management.controller;

import com.campus.management.entity.SysFile;
import com.campus.management.entity.SysUser;
import com.campus.management.service.FileService;
import com.campus.management.service.ProfileCacheService;
import com.campus.management.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/files")
public class FileController {
    private final FileService fileService;
    private final UserService userService;
    private final ProfileCacheService profileCacheService;

    public FileController(FileService fileService,
                          UserService userService,
                          ProfileCacheService profileCacheService) {
        this.fileService = fileService;
        this.userService = userService;
        this.profileCacheService = profileCacheService;
    }

    // 用户头像上传
    @PostMapping("/avatar")
    public String uploadAvatar(MultipartFile file,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            SysUser currentUser = userService.findByUsername(authentication.getName());
            // 保存文件并获取存储路径
            SysFile savedFile = fileService.saveFile(file, currentUser.getId(), "avatar");
            // 更新用户头像路径
            currentUser.setAvatar(savedFile.getFilePath());
            userService.updateById(currentUser);
            // 清除用户资料缓存
            profileCacheService.evictProfileCache(currentUser.getId());
            redirectAttributes.addFlashAttribute("successMessage", "头像上传成功");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/user/profile";
    }

    // 活动图片上传
    @PostMapping("/activity/{activityId}/image")
    public String uploadActivityImage(@PathVariable Long activityId,
                                      MultipartFile file,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        try {
            SysUser currentUser = userService.findByUsername(authentication.getName());
            // 保存活动图片文件
            SysFile savedFile = fileService.saveFile(file, currentUser.getId(), "activity");
            redirectAttributes.addFlashAttribute("successMessage", "活动图片上传成功，路径：" + savedFile.getFilePath());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/activities/" + activityId;
    }
}
