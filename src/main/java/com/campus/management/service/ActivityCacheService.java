package com.campus.management.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.management.dto.ActivityCacheItem;
import com.campus.management.dto.ActivityListCacheData;
import com.campus.management.entity.Activity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class ActivityCacheService {
    private static final Logger log = LoggerFactory.getLogger(ActivityCacheService.class);
    // 活动详情缓存key前缀
    private static final String ACTIVITY_DETAIL_KEY_PREFIX = "activity:detail:";
    // 活动详情列表key前缀
    private static final String ACTIVITY_LIST_KEY_PREFIX = "activity:list:";
    // 缓存过期时间
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ActivityCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Activity getActivityDetail(Long activityId) {
        String cacheKey = ACTIVITY_DETAIL_KEY_PREFIX + activityId;
        String cachedJson = readCacheSafely(cacheKey); // 读取缓存
        if (cachedJson == null || cachedJson.isBlank()) {
            return null; // 未命中返回NULL
        }
        try {
            return toActivity(objectMapper.readValue(cachedJson, ActivityCacheItem.class));
        } catch (JsonProcessingException exception) {
            log.error("活动详情缓存反序列化失败, key={}", cacheKey, exception);
            evictActivityDetailCache(activityId); // 清理脏数据
            return null;
        }
    }

    public void cacheActivityDetail(Activity activity) {
        String cacheKey = ACTIVITY_DETAIL_KEY_PREFIX + activity.getId();
        try {
            String cacheValue = objectMapper.writeValueAsString(toCacheItem(activity));
            // 写入Redis并设置TTL
            stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL);
        } catch (JsonProcessingException exception) {
            log.error("活动详情缓存序列化失败, key={}", cacheKey, exception);
            evictActivityDetailCache(activity.getId());
        } catch (RuntimeException exception) {
            log.error("活动详情缓存写入失败, key={}", cacheKey, exception);
            evictActivityDetailCache(activity.getId());
        }
    }

    public Page<Activity> getActivityList(String keyword, String location, int pageNum) {
        String cacheKey = buildActivityListCacheKey(keyword, location, pageNum);
        String cachedJson = readCacheSafely(cacheKey);
        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }
        try {
            // 反序列化分页结构
            ActivityListCacheData cacheData = objectMapper.readValue(cachedJson, ActivityListCacheData.class);
            // 重建分页对象
            Page<Activity> pageData = new Page<>(cacheData.getCurrent(), cacheData.getSize(), cacheData.getTotal());
            pageData.setPages(cacheData.getPages());
            List<Activity> records = cacheData.getRecords() == null
                ? Collections.emptyList()
                : cacheData.getRecords().stream().map(this::toActivity).toList();
            pageData.setRecords(records);
            return pageData;
        } catch (JsonProcessingException exception) {
            log.error("活动列表缓存反序列化失败, key={}", cacheKey, exception);
            evictActivityListCache(keyword, location, pageNum);
            return null;
        }
    }

    public void cacheActivityList(String keyword, String location, int pageNum, Page<Activity> pageData) {
        String cacheKey = buildActivityListCacheKey(keyword, location, pageNum);
        ActivityListCacheData cacheData = new ActivityListCacheData();
        // 分页元数据缓存，避免重复计算
        cacheData.setCurrent(pageData.getCurrent());
        cacheData.setSize(pageData.getSize());
        cacheData.setTotal(pageData.getTotal());
        cacheData.setPages(pageData.getPages());
        // 数据列表压缩为Cache DTO，减少冗余字段
        cacheData.setRecords(pageData.getRecords() == null
            ? Collections.emptyList()
            : pageData.getRecords().stream().map(this::toCacheItem).toList());
        try {
            String cacheValue = objectMapper.writeValueAsString(cacheData);
            stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL);
        } catch (JsonProcessingException exception) {
            log.error("活动列表缓存序列化失败, key={}", cacheKey, exception);
            evictActivityListCache(keyword, location, pageNum);
        } catch (RuntimeException exception) {
            log.error("活动列表缓存写入失败, key={}", cacheKey, exception);
            evictActivityListCache(keyword, location, pageNum);
        }
    }

    public void evictActivityDetailCache(Long activityId) {
        deleteSafely(ACTIVITY_DETAIL_KEY_PREFIX + activityId);
    }

    public void evictActivityListCache(String keyword, String location, int pageNum) {
        deleteSafely(buildActivityListCacheKey(keyword, location, pageNum));
    }

    public void evictAllActivityCaches() {
        deleteByPattern(ACTIVITY_DETAIL_KEY_PREFIX + "*");
        deleteByPattern(ACTIVITY_LIST_KEY_PREFIX + "*");
    }

    private ActivityCacheItem toCacheItem(Activity activity) {
        ActivityCacheItem item = new ActivityCacheItem();
        item.setId(activity.getId());
        item.setTitle(activity.getTitle());
        item.setContent(activity.getContent());
        item.setLocation(activity.getLocation());
        item.setStartTime(activity.getStartTime());
        item.setEndTime(activity.getEndTime());
        item.setMaxPeople(activity.getMaxPeople());
        item.setCurrentPeople(activity.getCurrentPeople());
        item.setImagePath(activity.getImagePath());
        item.setStatus(activity.getStatus());
        item.setIsDeleted(activity.getIsDeleted());
        return item;
    }

    private Activity toActivity(ActivityCacheItem item) {
        Activity activity = new Activity();
        activity.setId(item.getId());
        activity.setTitle(item.getTitle());
        activity.setContent(item.getContent());
        activity.setLocation(item.getLocation());
        activity.setStartTime(item.getStartTime());
        activity.setEndTime(item.getEndTime());
        activity.setMaxPeople(item.getMaxPeople());
        activity.setCurrentPeople(item.getCurrentPeople());
        activity.setImagePath(item.getImagePath());
        activity.setStatus(item.getStatus());
        activity.setIsDeleted(item.getIsDeleted());
        return activity;
    }

    private String buildActivityListCacheKey(String keyword, String location, int pageNum) {
        String rawKey = normalize(keyword) + "|" + normalize(location) + "|" + pageNum;
        // MD5压缩，避免key过长
        return ACTIVITY_LIST_KEY_PREFIX + DigestUtils.md5DigestAsHex(rawKey.getBytes(StandardCharsets.UTF_8));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim(); // 防止null污染key结构
    }

    private String readCacheSafely(String cacheKey) {
        try {
            return stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (RedisSystemException exception) {
            log.error("活动缓存读取失败, key={}", cacheKey, exception);
            deleteSafely(cacheKey);
            return null;
        } catch (RuntimeException exception) {
            log.error("活动缓存读取过程中发生未知异常, key={}", cacheKey, exception);
            return null;
        }
    }

    private void deleteSafely(String cacheKey) {
        try {
            stringRedisTemplate.delete(cacheKey);
        } catch (RuntimeException exception) {
            log.error("活动缓存删除失败, key={}", cacheKey, exception);
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern); // 模糊匹配
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys); // 批量删除
            }
        } catch (RuntimeException exception) {
            log.error("活动缓存批量删除失败, pattern={}", pattern, exception);
        }
    }
}

