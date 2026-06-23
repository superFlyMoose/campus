package com.campus.management.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardCacheService {

    private static final String SUMMARY_CACHE_KEY = "dashboard:summary";
    private static final String HOT_ACTIVITY_CACHE_KEY = "dashboard:hotActivities";
    private static final String CHART_CACHE_KEY = "dashboard:chartData";

    private final StringRedisTemplate stringRedisTemplate;

    public DashboardCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void evictDashboardCache() {
        stringRedisTemplate.delete(SUMMARY_CACHE_KEY);
        stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
        stringRedisTemplate.delete(CHART_CACHE_KEY);
    }
}
