package com.campus.management.dto;

import java.time.LocalDateTime;
import lombok.Data;


/**
 * 热门活动缓存数据项
 * 用于缓存系统中的热门活动列表数据
 */
@Data
public class HotActivityCacheItem {
    private Long id;
    private String title;
    private String location;
    private LocalDateTime startTime;
    private Integer currentPeople;
    private Integer maxPeople;
}
