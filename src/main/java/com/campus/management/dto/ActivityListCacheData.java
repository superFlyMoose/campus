package com.campus.management.dto;

import java.util.List;
import lombok.Data;

/**
 * 活动列表缓存数据对象
 * 用于缓存活动列表分页数据
 */
@Data
public class ActivityListCacheData {

    private long current;
    private long size;
    private long total;
    private long pages;
    private List<ActivityCacheItem> records;
}
