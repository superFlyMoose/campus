package com.campus.management.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardCacheService {
    // 统计摘要缓存Key
    private static final String SUMMARY_CACHE_KEY = "dashboard:summary";
    // 热门活动列表缓存Key
    private static final String HOT_ACTIVITY_CACHE_KEY = "dashboard:hotActivities";
    // 图表数据缓存Key
    private static final String CHART_CACHE_KEY = "dashboard:chartData";
    private final StringRedisTemplate stringRedisTemplate;

    public DashboardCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 清除仪表盘相关缓存
     * 采用“失效删除”而非“主动更新”，避免复杂一致性问题
     */
    public void evictDashboardCache() {
        stringRedisTemplate.delete(SUMMARY_CACHE_KEY);
        stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
        stringRedisTemplate.delete(CHART_CACHE_KEY);
    }
}
