package com.campus.management.service;

import com.campus.management.dto.ProfileCacheData;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
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

    public ProfileCacheData getProfile(Long userId) {
        String cachedJson = readProfileCacheSafely(userId);
        if (cachedJson == null || cachedJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(cachedJson, ProfileCacheData.class);
        } catch (JsonProcessingException exception) {
            log.error("Profile cache parse failed, key={}", buildProfileCacheKey(userId), exception);
            evictProfileCache(userId);
            return null;
        }
    }

    public void cacheProfile(SysUser user, List<ActivityRegistration> registrations) {
        ProfileCacheData cacheData = new ProfileCacheData();
        cacheData.setUserId(user.getId());
        cacheData.setUsername(user.getUsername());
        cacheData.setRealName(user.getRealName());
        cacheData.setRole(user.getRole());
        cacheData.setAvatar(user.getAvatar());
        cacheData.setRegistrations(toRegistrationCacheItems(registrations));
        String cacheKey = buildProfileCacheKey(user.getId());
        try {
            String cacheValue = objectMapper.writeValueAsString(cacheData);
            stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, CACHE_TTL);
            log.info("Profile cache stored, key={}, registrations={}", cacheKey, cacheData.getRegistrations().size());
        } catch (JsonProcessingException exception) {
            log.error("Profile cache serialization failed, key={}", cacheKey, exception);
            evictProfileCache(user.getId());
        } catch (RuntimeException exception) {
            log.error("Profile cache write failed, key={}", cacheKey, exception);
            evictProfileCache(user.getId());
        }
    }

    public void evictProfileCache(Long userId) {
        String cacheKey = buildProfileCacheKey(userId);
        try {
            stringRedisTemplate.delete(cacheKey);
            log.info("Profile cache evicted, key={}", cacheKey);
        } catch (RuntimeException exception) {
            log.error("Profile cache eviction failed, key={}", cacheKey, exception);
        }
    }

    private String readProfileCacheSafely(Long userId) {
        String cacheKey = buildProfileCacheKey(userId);
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            log.info("Profile cache read, key={}, hit={}", cacheKey, cachedJson != null && !cachedJson.isBlank());
            return cachedJson;
        } catch (RedisSystemException exception) {
            log.error("Profile cache read failed, key={}", cacheKey, exception);
            evictProfileCache(userId);
            return null;
        } catch (RuntimeException exception) {
            log.error("Profile cache unexpected read failure, key={}", cacheKey, exception);
            return null;
        }
    }

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

    private String buildProfileCacheKey(Long userId) {
        return PROFILE_CACHE_KEY_PREFIX + userId;
    }
}

