package com.campus.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.management.config.RabbitMqConfig;
import com.campus.management.dto.ActivityForm;
import com.campus.management.dto.ActivityMessage;
import com.campus.management.entity.Activity;
import com.campus.management.mapper.ActivityMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ActivityService extends ServiceImpl<ActivityMapper, Activity> {

    private final ActivityMessageProducer activityMessageProducer;
    private final DashboardCacheService dashboardCacheService;
    private final ActivityCacheService activityCacheService;
    private final FileService fileService;

    public ActivityService(ActivityMessageProducer activityMessageProducer,
                           DashboardCacheService dashboardCacheService,
                           ActivityCacheService activityCacheService,
                           FileService fileService) {
        this.activityMessageProducer = activityMessageProducer;
        this.dashboardCacheService = dashboardCacheService;
        this.activityCacheService = activityCacheService;
        this.fileService = fileService;
    }

    public Page<Activity> searchActivities(String keyword, String location, int pageNum, int pageSize) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getIsDeleted, 0)
            .like(StringUtils.hasText(keyword), Activity::getTitle, keyword)
            .like(StringUtils.hasText(location), Activity::getLocation, location)
            .orderByDesc(Activity::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<Activity> listLatestActivities(int limit) {
        return lambdaQuery()
            .eq(Activity::getIsDeleted, 0)
            .orderByDesc(Activity::getStartTime)
            .last("limit " + limit)
            .list();
    }

    public long countActiveActivities() {
        return lambdaQuery().eq(Activity::getIsDeleted, 0).count();
    }

    public Activity getActivityOrThrow(Long id) {
        Activity activity = getById(id);
        if (activity == null || Integer.valueOf(1).equals(activity.getIsDeleted())) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    public void createActivity(ActivityForm form) {
        validateTime(form);
        Activity activity = new Activity();
        activity.setTitle(form.getTitle());
        activity.setContent(form.getContent());
        activity.setLocation(form.getLocation());
        activity.setStartTime(form.getStartTime());
        activity.setEndTime(form.getEndTime());
        activity.setMaxPeople(form.getMaxPeople());
        activity.setCurrentPeople(0);
        activity.setImagePath(form.getImagePath());
        activity.setStatus(0);
        activity.setIsDeleted(0);
        save(activity);
        dashboardCacheService.evictDashboardCache();
        activityCacheService.evictAllActivityCaches();
        sendActivityCreatedMessage(activity);
    }

    public void updateActivity(Long id, ActivityForm form) {
        validateTime(form);
        Activity activity = getActivityOrThrow(id);
        activity.setTitle(form.getTitle());
        activity.setContent(form.getContent());
        activity.setLocation(form.getLocation());
        activity.setStartTime(form.getStartTime());
        activity.setEndTime(form.getEndTime());
        activity.setMaxPeople(form.getMaxPeople());
        activity.setImagePath(form.getImagePath());
        updateById(activity);
        dashboardCacheService.evictDashboardCache();
        activityCacheService.evictAllActivityCaches();
    }

    public void removeActivity(Long id) {
        removeById(id);
        dashboardCacheService.evictDashboardCache();
        activityCacheService.evictAllActivityCaches();
    }

    public boolean canRegister(Activity activity) {
        return activity.getStartTime() != null
            && activity.getStartTime().isAfter(LocalDateTime.now())
            && activity.getCurrentPeople() < activity.getMaxPeople();
    }

    public void bindActivityImage(ActivityForm form, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }
        form.setImagePath(fileService.saveFile(imageFile, 0L, "activity").getFilePath());
    }

    private void validateTime(ActivityForm form) {
        if (form.getStartTime() != null && form.getEndTime() != null
            && !form.getEndTime().isAfter(form.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
    }

    private void sendActivityCreatedMessage(Activity activity) {
        ActivityMessage message = new ActivityMessage();
        message.setEventType("ACTIVITY_CREATED");
        message.setActivityId(activity.getId());
        message.setActivityTitle(activity.getTitle());
        message.setEventTime(LocalDateTime.now());
        message.setDescription("新活动发布成功");
        activityMessageProducer.send(RabbitMqConfig.ACTIVITY_CREATED_ROUTING_KEY, message);
    }
}


