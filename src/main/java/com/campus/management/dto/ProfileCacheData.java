package com.campus.management.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ProfileCacheData {

    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String avatar;
    private List<RegistrationCacheItem> registrations;

    @Data
    public static class RegistrationCacheItem {
        private Long id;
        private Long activityId;
        private Long userId;
        private LocalDateTime registrationTime;
        private Integer isDeleted;
    }
}
