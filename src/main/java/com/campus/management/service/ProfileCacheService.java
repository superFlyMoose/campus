package com.campus.management.service;

import com.campus.management.dto.ProfileCacheData;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProfileCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProfileCacheService.class);
    private static final String PROFILE_CACHE_KEY_PREFIX = "profile:user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProfileCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取用户画像缓存
     * @param userId 用户ID
     * @return ProfileCacheData（缓存命中）或 null（未命中/异常）
     */
    public ProfileCacheData getProfile(Long userId) {
        String cachedJson = readProfileCacheSafely(userId);
        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, ProfileCacheData.class);
        } catch (JsonProcessingException exception) {
            log.error("用户资料缓存反序列化失败, key={}", buildProfileCacheKey(userId), exception);
            evictProfileCache(userId);
            return null;
        }
    }

    /**
     * 写入用户画像缓存
     */
    public void cacheProfile(SysUser user, List<ActivityRegistration> registrations) {
        ProfileCacheData cacheData = new ProfileCacheData();
        // 用户基础信息
        cacheData.setUserId(user.getId());
        cacheData.setUsername(user.getUsername());
        cacheData.setRealName(user.getRealName());
        cacheData.setRole(user.getRole());
        cacheData.setAvatar(user.getAvatar());
        // 报名记录
        cacheData.setRegistrations(toRegistrationCacheItems(registrations));
        cacheData.setDailyRegistrationChart(buildDailyRegistrationChart(registrations));
        String cacheKey = buildProfileCacheKey(user.getId());
        try {
            String cacheValue = objectMapper.writeValueAsString(cacheData);
            stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL);
        } catch (JsonProcessingException exception) {
            log.error("用户资料缓存序列化失败, key={}", cacheKey, exception);
            evictProfileCache(user.getId());
        } catch (RuntimeException exception) {
            log.error("用户资料缓存写入失败, key={}", cacheKey, exception);
            evictProfileCache(user.getId());
        }
    }

    /**
     * 删除用户画像缓存
     */
    public void evictProfileCache(Long userId) {
        String cacheKey = buildProfileCacheKey(userId);
        try {
            stringRedisTemplate.delete(cacheKey);
        } catch (RuntimeException exception) {
            log.error("用户资料缓存清除失败, key={}", cacheKey, exception);
        }
    }

    /**
     * 安全读取缓存
     *
     * 容错策略：
     * Redis异常 → 返回 null（降级走DB）
     * 自动删除可能损坏缓存
     */
    private String readProfileCacheSafely(Long userId) {
        String cacheKey = buildProfileCacheKey(userId);
        try {
            return stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (RedisSystemException exception) {
            log.error("用户资料缓存读取失败, key={}", cacheKey, exception);
            evictProfileCache(userId);
            return null;
        } catch (RuntimeException exception) {
            log.error("用户资料缓存读取过程中发生未知异常, key={}", cacheKey, exception);
            return null;
        }
    }

    /**
     * 报名记录->缓存DTO转换
     */
    private List<ProfileCacheData.RegistrationCacheItem> toRegistrationCacheItems(List<ActivityRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return Collections.emptyList();
        }
        return registrations.stream()
            .map(registration -> {
                ProfileCacheData.RegistrationCacheItem item = new ProfileCacheData.RegistrationCacheItem();
                item.setId(registration.getId());
                item.setActivityId(registration.getActivityId());
                item.setUserId(registration.getUserId());
                item.setRegistrationTime(registration.getRegistrationTime());
                item.setIsDeleted(registration.getIsDeleted());
                return item;
            })
            .toList();
    }

    /**
     * 构建用户报名行为时间分布图（24小时粒度）
     */
    private ProfileCacheData.DailyRegistrationChart buildDailyRegistrationChart(List<ActivityRegistration> registrations) {
        ProfileCacheData.DailyRegistrationChart chart = new ProfileCacheData.DailyRegistrationChart();
        LocalDate today = LocalDate.now();
        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        long todayCount = 0L;

        for (int hour = 0; hour < 24; hour++) {
            labels.add(String.format("%02d:00", hour));
            final int currentHour = hour;
            long count = registrations == null ? 0L : registrations.stream()
                .filter(registration -> registration.getRegistrationTime() != null)
                .filter(registration -> registration.getRegistrationTime().toLocalDate().isEqual(today))
                .filter(registration -> registration.getRegistrationTime().getHour() == currentHour)
                .count();
            counts.add(count);
            todayCount += count;
        }

        chart.setLabels(labels);
        chart.setCounts(counts);
        chart.setTodayCount(todayCount);
        return chart;
    }

    private String buildProfileCacheKey(Long userId) {
        return PROFILE_CACHE_KEY_PREFIX + userId;
    }
}


