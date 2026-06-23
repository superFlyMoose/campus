package com.campus.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.management.entity.Activity;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final String SUMMARY_CACHE_KEY = "dashboard:summary";
    private static final String HOT_ACTIVITY_CACHE_KEY = "dashboard:hotActivities";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String USER_TOTAL_LABEL = "用户总数";
    private static final String ACTIVITY_TOTAL_LABEL = "活动总数";
    private static final String REGISTRATION_TOTAL_LABEL = "报名总数";

    private final UserService userService;
    private final ActivityService activityService;
    private final RegistrationService registrationService;
    private final StringRedisTemplate stringRedisTemplate;

    public DashboardService(UserService userService,
                            ActivityService activityService,
                            RegistrationService registrationService,
                            StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.activityService = activityService;
        this.registrationService = registrationService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Map<String, Long> getSummary() {
        List<String> cachedValues = stringRedisTemplate.opsForList().range(SUMMARY_CACHE_KEY, 0, -1);
        if (cachedValues != null && cachedValues.size() == 3) {
            Map<String, Long> cachedSummary = new LinkedHashMap<>();
            cachedSummary.put(USER_TOTAL_LABEL, parseLong(cachedValues.get(0)));
            cachedSummary.put(ACTIVITY_TOTAL_LABEL, parseLong(cachedValues.get(1)));
            cachedSummary.put(REGISTRATION_TOTAL_LABEL, parseLong(cachedValues.get(2)));
            return cachedSummary;
        }

        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put(USER_TOTAL_LABEL, userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getIsDeleted, 0)));
        summary.put(ACTIVITY_TOTAL_LABEL, activityService.countActiveActivities());
        summary.put(REGISTRATION_TOTAL_LABEL, registrationService.count(new LambdaQueryWrapper<ActivityRegistration>().eq(ActivityRegistration::getIsDeleted, 0)));
        cacheSummary(summary);
        return summary;
    }

    public List<Activity> getHotActivities() {
        List<String> cachedIds = stringRedisTemplate.opsForList().range(HOT_ACTIVITY_CACHE_KEY, 0, -1);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            List<Activity> activities = new ArrayList<>();
            for (String cachedId : cachedIds) {
                try {
                    Activity activity = activityService.getById(Long.parseLong(cachedId));
                    if (activity != null && Integer.valueOf(0).equals(activity.getIsDeleted())) {
                        activities.add(activity);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (!activities.isEmpty()) {
                return activities;
            }
        }

        List<Activity> activities = activityService.listLatestActivities(6);
        cacheHotActivities(activities);
        return activities;
    }

    public Map<String, List<?>> getChartData() {
        LinkedHashMap<String, List<?>> chartData = new LinkedHashMap<>();
        chartData.put("labels", buildDateLabels());
        chartData.put("activityCounts", buildDailyActivityCounts());
        chartData.put("registrationCounts", buildDailyRegistrationCounts());
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
            String.valueOf(summary.getOrDefault(USER_TOTAL_LABEL, 0L)),
            String.valueOf(summary.getOrDefault(ACTIVITY_TOTAL_LABEL, 0L)),
            String.valueOf(summary.getOrDefault(REGISTRATION_TOTAL_LABEL, 0L))
        );
        stringRedisTemplate.expire(SUMMARY_CACHE_KEY, CACHE_TTL);
    }

    private void cacheHotActivities(List<Activity> activities) {
        stringRedisTemplate.delete(HOT_ACTIVITY_CACHE_KEY);
        if (activities.isEmpty()) {
            return;
        }
        List<String> values = activities.stream()
            .map(Activity::getId)
            .map(String::valueOf)
            .toList();
        stringRedisTemplate.opsForList().rightPushAll(HOT_ACTIVITY_CACHE_KEY, values);
        stringRedisTemplate.expire(HOT_ACTIVITY_CACHE_KEY, CACHE_TTL);
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }
}

