package com.campus.management.dto;

import java.time.LocalDateTime;
import lombok.Data;

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
