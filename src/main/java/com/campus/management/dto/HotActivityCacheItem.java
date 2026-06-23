package com.campus.management.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HotActivityCacheItem {

    private Long id;
    private String title;
    private String location;
    private LocalDateTime startTime;
    private Integer currentPeople;
    private Integer maxPeople;
}
