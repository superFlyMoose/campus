package com.campus.management.dto;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 活动缓存数据对象
 * 用于在缓存层中存储活动信息，
 * 避免频繁访问数据库。
 */
@Data
public class ActivityCacheItem {
    private Long id;
    private String title;
    private String content;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer maxPeople;
    private Integer currentPeople;
    private String imagePath;
    private Integer status;
    private Integer isDeleted;
}
