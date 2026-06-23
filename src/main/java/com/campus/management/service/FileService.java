package com.campus.management.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.management.entity.SysFile;
import com.campus.management.mapper.SysFileMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService extends ServiceImpl<SysFileMapper, SysFile> {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public SysFile saveFile(MultipartFile file, Long uploadUserId, String category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String safeExtension = extension == null ? "dat" : extension.toLowerCase();
        if (!safeExtension.matches("jpg|jpeg|png|gif|webp")) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、gif、webp 格式");
        }
        try {
            Path categoryPath = Paths.get(uploadDir, category);
            Files.createDirectories(categoryPath);
            String storedName = UUID.randomUUID() + "." + safeExtension;
            Path targetPath = categoryPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            SysFile sysFile = new SysFile();
            sysFile.setFileName(originalFilename);
            sysFile.setFilePath("/uploads/" + category + "/" + storedName);
            sysFile.setFileSize(file.getSize());
            sysFile.setUploadUserId(uploadUserId);
            sysFile.setCreateTime(LocalDateTime.now());
            save(sysFile);
            return sysFile;
        } catch (IOException ex) {
            throw new IllegalStateException("文件保存失败", ex);
        }
    }
}
