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
    private DailyRegistrationChart dailyRegistrationChart;

    @Data
    public static class RegistrationCacheItem {
        private Long id;
        private Long activityId;
        private Long userId;
        private LocalDateTime registrationTime;
        private Integer isDeleted;
    }

    @Data
    public static class DailyRegistrationChart {
        private List<String> labels;
        private List<Long> counts;
        private Long todayCount;
    }
}
