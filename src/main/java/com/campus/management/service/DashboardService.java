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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final String SUMMARY_CACHE_KEY = "dashboard:summary";
    private static final String HOT_ACTIVITY_CACHE_KEY = "dashboard:hotActivities";
    private static final String CHART_CACHE_KEY = "dashboard:chartData";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final UserService userService;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardService(UserService userService, ActivityService activityService, RegistrationService registrationService, StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.userService = userService;
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取仪表盘核心统计摘要（用户数 / 活动数 / 报名数）
     */
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

    /**
     * 获取热门活动列表（首页展示模块）
     */
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

    /**
     * 获取图表数据（近7天趋势）
     * 计算成本较高，因此必须缓存
     */
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

    /**
     * 构建近7天日期标签（用于图表X轴）
     */
    private List<String> buildDateLabels() {
        LinkedList<String> labels = new LinkedList<>();
        for (int index = 6; index >= 0; index--) {
            labels.add(LocalDate.now().minusDays(index).toString());
        }
        return labels;
    }

    /**
     * 统计近7天每日活动创建数量
     *
     * 查询方式：
     * 按createTime进行时间范围过滤
     * 每天执行一次COUNT
     */
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

    /**
     * 统计近7天每日报名数量
     */
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

    /**
     * 写入Summary缓存
     */
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

    /**
     * 写入热门活动缓存
     */
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
            log.error("最新活动缓存写入失败，key={}", HOT_ACTIVITY_CACHE_KEY, exception);
            stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
        }
    }

    /**
     * 安全读取热门活动缓存
     */
    private String readHotActivitiesCacheSafely() {
        try {
            return stringRedisTemplate.opsForValue().get(HOT_ACTIVITY_CACHE_KEY);
        } catch (RedisSystemException exception) {
            log.error("最新活动缓存读取失败，key={}", HOT_ACTIVITY_CACHE_KEY, exception);
            stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
            return null;
        }
    }

    /**
     * 热门活动缓存反序列化
     */
    private List<Activity> parseCachedActivities(String cachedJson) {
        try {
            List<HotActivityCacheItem> items = objectMapper.readValue(
                cachedJson,
                objectMapper.getTypeFactory().constructCollectionType(List.class, HotActivityCacheItem.class)
            );
            return items.stream().map(this::toActivity).collect(Collectors.toList());
        } catch (JsonProcessingException exception) {
            log.error("最新活动缓存反序列化失败，key={}", HOT_ACTIVITY_CACHE_KEY, exception);
            stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
            return Collections.emptyList();
        }
    }

    /**
     * 降低缓存体积
     */
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

    /**
     * 安全读取图标缓存
     */
    private String readChartCacheSafely() {
        try {
            return stringRedisTemplate.opsForValue().get(CHART_CACHE_KEY);
        } catch (RedisSystemException exception) {
            log.error("图表数据缓存读取失败，key={}", CHART_CACHE_KEY, exception);
            stringRedisTemplate.delete(CHART_CACHE_KEY);
            return null;
        }
    }

    /**
     * 写入图表缓存
     */
    private void cacheChartData(Map<String, List<?>> chartData) {
        DashboardChartCacheData cacheData = new DashboardChartCacheData();
        cacheData.setLabels(castStringList(chartData.get("labels")));
        cacheData.setActivityCounts(castLongList(chartData.get("activityCounts")));
        cacheData.setRegistrationCounts(castLongList(chartData.get("registrationCounts")));
        try {
            stringRedisTemplate.opsForValue().set(CHART_CACHE_KEY, objectMapper.writeValueAsString(cacheData), CACHE_TTL);
        } catch (JsonProcessingException exception) {
            log.error("图表数据缓存写入失败，key={}", CHART_CACHE_KEY, exception);
            stringRedisTemplate.delete(CHART_CACHE_KEY);
        }
    }

    /**
     * 图表缓存解析
     */
    private Map<String, List<?>> parseChartData(String cachedJson) {
        try {
            DashboardChartCacheData cacheData = objectMapper.readValue(cachedJson, DashboardChartCacheData.class);
            LinkedHashMap<String, List<?>> chartData = new LinkedHashMap<>();
            chartData.put("labels", cacheData.getLabels() == null ? Collections.emptyList() : cacheData.getLabels());
            chartData.put("activityCounts", cacheData.getActivityCounts() == null ? Collections.emptyList() : cacheData.getActivityCounts());
            chartData.put("registrationCounts", cacheData.getRegistrationCounts() == null ? Collections.emptyList() : cacheData.getRegistrationCounts());
            return chartData;
        } catch (JsonProcessingException exception) {
            log.error("图表数据缓存反序列化失败，key={}", CHART_CACHE_KEY, exception);
            stringRedisTemplate.delete(CHART_CACHE_KEY);
            return Collections.emptyMap();
        }
    }

    /**
     * 安全类型转换：List<?> -> List<String>
     */
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
            log.warn("缓存数据格式异常，无法转换为Long，value={}", value);
            return 0L;
        }
    }
}

