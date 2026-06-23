package com.campus.management.dto;

import java.util.List;
import lombok.Data;

@Data
public class ActivityListCacheData {

    private long current;
    private long size;
    private long total;
    private long pages;
    private List<ActivityCacheItem> records;
}
