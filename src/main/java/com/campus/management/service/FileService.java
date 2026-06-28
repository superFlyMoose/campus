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
        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        // 默认拓展名兜底
        String safeExtension = extension == null ? "dat" : extension.toLowerCase();
        if (!safeExtension.matches("jpg|jpeg|png|gif|webp")) {
            throw new IllegalArgumentException("仅支持 jpg、jpeg、png、gif、webp 格式");
        }
        try {
            // 构建分类存储目录
            Path categoryPath = Paths.get(uploadDir, category);
            Files.createDirectories(categoryPath);
            // 生成存储文件名，UUID防冲突
            String storedName = UUID.randomUUID() + "." + safeExtension;
            // 构建最终存储路径
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
