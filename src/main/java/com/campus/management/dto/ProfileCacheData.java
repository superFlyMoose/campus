package com.campus.management.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 用户个人中心缓存数据对象
 * 用于缓存用户个人中心页面所需的完整数据结构
 */
@Data
public class ProfileCacheData {

    private Long userId;
    private String username;
    private String realName;
    private String role;
    private String avatar;
    private List<RegistrationCacheItem> registrations;
    private DailyRegistrationChart dailyRegistrationChart;

    /**
     * 报名记录缓存项
     */
    @Data
    public static class RegistrationCacheItem {
        private Long id;
        private Long activityId;
        private Long userId;
        private LocalDateTime registrationTime;
        private Integer isDeleted;
    }

    /**
     * 用户每日报名统计图数据结构
     * 用于前端图表展示用户报名行为的时间分布趋势
     */
    @Data
    public static class DailyRegistrationChart {
        private List<String> labels;
        private List<Long> counts;
        private Long todayCount;
    }
}
