package com.campus.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.management.dto.DashboardChartCacheData;
import com.campus.management.dto.HotActivityCacheItem;
import com.campus.management.entity.Activity;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final String SUMMARY_CACHE_KEY = "dashboard:summary";
    private static final String HOT_ACTIVITY_CACHE_KEY = "dashboard:hotActivities";
    private static final String CHART_CACHE_KEY = "dashboard:chartData";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final UserService userService;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardService(UserService userService,
                            ActivityService activityService,
                            RegistrationService registrationService,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.userService = userService;
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Long> getSummary() {
        List<String> cachedValues = stringRedisTemplate.opsForList().range(SUMMARY_CACHE_KEY, 0, -1);
        if (cachedValues != null && cachedValues.size() == 3) {
            Map<String, Long> cachedSummary = new LinkedHashMap<>();
            cachedSummary.put("用户总数", parseLong(cachedValues.get(0)));
            cachedSummary.put("活动总数", parseLong(cachedValues.get(1)));
            cachedSummary.put("报名总数", parseLong(cachedValues.get(2)));
            return cachedSummary;
        }

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("用户总数", userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getIsDeleted, 0)));
        summary.put("活动总数", activityService.countActiveActivities());
        summary.put("报名总数", registrationService.count(new LambdaQueryWrapper<ActivityRegistration>().eq(ActivityRegistration::getIsDeleted, 0)));
        cacheSummary(summary);
        return summary;
    }

    public List<Activity> getHotActivities() {
        String cachedJson = readHotActivitiesCacheSafely();
        if (cachedJson != null && !cachedJson.isBlank()) {
            List<Activity> cachedActivities = parseCachedActivities(cachedJson);
            if (!cachedActivities.isEmpty()) {
                return cachedActivities;
            }
        }

        List<Activity> activities = activityService.listLatestActivities(6);
        cacheHotActivities(activities);
        return activities;
    }

    public Map<String, List<?>> getChartData() {
        String cachedJson = readChartCacheSafely();
        if (cachedJson != null && !cachedJson.isBlank()) {
            Map<String, List<?>> cachedChartData = parseChartData(cachedJson);
            if (!cachedChartData.isEmpty()) {
                return cachedChartData;
            }
        }

        LinkedHashMap<String, List<?>> chartData = new LinkedHashMap<>();
        chartData.put("labels", buildDateLabels());
        chartData.put("activityCounts", buildDailyActivityCounts());
        chartData.put("registrationCounts", buildDailyRegistrationCounts());
        cacheChartData(chartData);
        return chartData;
    }

    private List<String> buildDateLabels() {
        LinkedList<String> labels = new LinkedList<>();
        for (int index = 6; index >= 0; index--) {
            labels.add(LocalDate.now().minusDays(index).toString());
        }
        return labels;
    }

    private List<Long> buildDailyActivityCounts() {
        LinkedList<Long> counts = new LinkedList<>();
        for (int index = 6; index >= 0; index--) {
            LocalDate day = LocalDate.now().minusDays(index);
            counts.add(activityService.lambdaQuery()
                .eq(Activity::getIsDeleted, 0)
                .ge(Activity::getCreateTime, day.atStartOfDay())
                .lt(Activity::getCreateTime, day.plusDays(1).atStartOfDay())
                .count());
        }
        return counts;
    }

    private List<Long> buildDailyRegistrationCounts() {
        LinkedList<Long> counts = new LinkedList<>();
        for (int index = 6; index >= 0; index--) {
            LocalDate day = LocalDate.now().minusDays(index);
            counts.add(registrationService.lambdaQuery()
                .eq(ActivityRegistration::getIsDeleted, 0)
                .ge(ActivityRegistration::getRegistrationTime, day.atStartOfDay())
                .lt(ActivityRegistration::getRegistrationTime, day.plusDays(1).atStartOfDay())
                .count());
        }
        return counts;
    }

    private void cacheSummary(Map<String, Long> summary) {
        stringRedisTemplate.delete(SUMMARY_CACHE_KEY);
        stringRedisTemplate.opsForList().rightPushAll(
            SUMMARY_CACHE_KEY,
            String.valueOf(summary.getOrDefault("用户总数", 0L)),
            String.valueOf(summary.getOrDefault("活动总数", 0L)),
            String.valueOf(summary.getOrDefault("报名总数", 0L))
        );
        stringRedisTemplate.expire(SUMMARY_CACHE_KEY, CACHE_TTL);
    }

    private void cacheHotActivities(List<Activity> activities) {
        stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
        if (activities.isEmpty()) {
            return;
        }

        List<HotActivityCacheItem> values = activities.stream()
            .map(this::toCacheItem)
            .collect(Collectors.toList());
        try {
            stringRedisTemplate.opsForValue().set(HOT_ACTIVITY_CACHE_KEY, objectMapper.writeValueAsString(values), CACHE_TTL);
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
        }
    }

    private String readHotActivitiesCacheSafely() {
        try {
            return stringRedisTemplate.opsForValue().get(HOT_ACTIVITY_CACHE_KEY);
        } catch (RedisSystemException exception) {
            stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
            return null;
        }
    }

    private List<Activity> parseCachedActivities(String cachedJson) {
        try {
            List<HotActivityCacheItem> items = objectMapper.readValue(
                cachedJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, HotActivityCacheItem.class)
            );
            return items.stream()
                .map(this::toActivity)
                .collect(Collectors.toList());
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
            return Collections.emptyList();
        }
    }

    private HotActivityCacheItem toCacheItem(Activity activity) {
        HotActivityCacheItem item = new HotActivityCacheItem();
        item.setId(activity.getId());
        item.setTitle(activity.getTitle());
        item.setLocation(activity.getLocation());
        item.setStartTime(activity.getStartTime());
        item.setCurrentPeople(activity.getCurrentPeople());
        item.setMaxPeople(activity.getMaxPeople());
        return item;
    }

    private Activity toActivity(HotActivityCacheItem item) {
        Activity activity = new Activity();
        activity.setId(item.getId());
        activity.setTitle(item.getTitle());
        activity.setLocation(item.getLocation());
        activity.setStartTime(item.getStartTime());
        activity.setCurrentPeople(item.getCurrentPeople());
        activity.setMaxPeople(item.getMaxPeople());
        activity.setIsDeleted(0);
        return activity;
    }

    private String readChartCacheSafely() {
        try {
            return stringRedisTemplate.opsForValue().get(CHART_CACHE_KEY);
        } catch (RedisSystemException exception) {
            stringRedisTemplate.delete(CHART_CACHE_KEY);
            return null;
        }
    }

    private void cacheChartData(Map<String, List<?>> chartData) {
        DashboardChartCacheData cacheData = new DashboardChartCacheData();
        cacheData.setLabels(castStringList(chartData.get("labels")));
        cacheData.setActivityCounts(castLongList(chartData.get("activityCounts")));
        cacheData.setRegistrationCounts(castLongList(chartData.get("registrationCounts")));
        try {
            stringRedisTemplate.opsForValue().set(CHART_CACHE_KEY, objectMapper.writeValueAsString(cacheData), CACHE_TTL);
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(CHART_CACHE_KEY);
        }
    }

    private Map<String, List<?>> parseChartData(String cachedJson) {
        try {
            DashboardChartCacheData cacheData = objectMapper.readValue(cachedJson, DashboardChartCacheData.class);
            LinkedHashMap<String, List<?>> chartData = new LinkedHashMap<>();
            chartData.put("labels", cacheData.getLabels() == null ? Collections.emptyList() : cacheData.getLabels());
            chartData.put("activityCounts", cacheData.getActivityCounts() == null ? Collections.emptyList() : cacheData.getActivityCounts());
            chartData.put("registrationCounts", cacheData.getRegistrationCounts() == null ? Collections.emptyList() : cacheData.getRegistrationCounts());
            return chartData;
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(CHART_CACHE_KEY);
            return Collections.emptyMap();
        }
    }

    private List<String> castStringList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    private List<Long> castLongList(List<?> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
            .map(value -> {
                if (value instanceof Number number) {
                    return number.longValue();
                }
                return parseLong(String.valueOf(value));
            })
            .collect(Collectors.toList());
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}

