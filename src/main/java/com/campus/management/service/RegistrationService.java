package com.campus.management.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campus.management.config.RabbitMqConfig;
import com.campus.management.dto.ActivityMessage;
import com.campus.management.entity.Activity;
import com.campus.management.entity.ActivityRegistration;
import com.campus.management.entity.SysUser;
import com.campus.management.mapper.ActivityRegistrationMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService extends ServiceImpl<ActivityRegistrationMapper, ActivityRegistration> {

    private final ActivityService activityService;
    private final ActivityMessageProducer activityMessageProducer;
    private final DashboardCacheService dashboardCacheService;
    private final ProfileCacheService profileCacheService;
    private final ActivityCacheService activityCacheService;

    public RegistrationService(ActivityService activityService,
                               ActivityMessageProducer activityMessageProducer,
                               DashboardCacheService dashboardCacheService,
                               ProfileCacheService profileCacheService,
                               ActivityCacheService activityCacheService) {
        this.activityService = activityService;
        this.activityMessageProducer = activityMessageProducer;
        this.dashboardCacheService = dashboardCacheService;
        this.profileCacheService = profileCacheService;
        this.activityCacheService = activityCacheService;
    }

    public List<ActivityRegistration> getMyRegistrations(Long userId) {
        return lambdaQuery()
            .eq(ActivityRegistration::getUserId, userId)
            .eq(ActivityRegistration::getIsDeleted, 0)
            .orderByDesc(ActivityRegistration::getRegistrationTime)
            .list();
    }

    public Page<ActivityRegistration> pageMyRegistrations(Long userId, int pageNum, int pageSize) {
        Page<ActivityRegistration> page = new Page<>(Math.max(pageNum, 1), pageSize);
        return lambdaQuery()
            .eq(ActivityRegistration::getUserId, userId)
            .eq(ActivityRegistration::getIsDeleted, 0)
            .orderByDesc(ActivityRegistration::getRegistrationTime)
            .page(page);
    }

    @Transactional
    public void register(Long activityId, SysUser currentUser) {
        Activity activity = activityService.getActivityOrThrow(activityId);
        boolean exists = lambdaQuery()
            .eq(ActivityRegistration::getActivityId, activityId)
            .eq(ActivityRegistration::getUserId, currentUser.getId())
            .eq(ActivityRegistration::getIsDeleted, 0)
            .count() > 0;
        if (exists) {
            throw new IllegalArgumentException("您已报名该活动");
        }
        if (!activityService.canRegister(activity)) {
            throw new IllegalArgumentException("当前活动无法报名");
        }

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(currentUser.getId());
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setIsDeleted(0);
        save(registration);

        activity.setCurrentPeople(activity.getCurrentPeople() + 1);
        activityService.updateById(activity);
        dashboardCacheService.evictDashboardCache();
        profileCacheService.evictProfileCache(currentUser.getId());
        activityCacheService.evictAllActivityCaches();
        sendRegistrationCreatedMessage(activity, currentUser);
    }

    @Transactional
    public void cancel(Long registrationId, SysUser currentUser) {
        ActivityRegistration registration = getOne(new LambdaQueryWrapper<ActivityRegistration>()
            .eq(ActivityRegistration::getId, registrationId)
            .eq(ActivityRegistration::getUserId, currentUser.getId())
            .eq(ActivityRegistration::getIsDeleted, 0)
            .last("limit 1"));
        if (registration == null) {
            throw new IllegalArgumentException("报名记录不存在");
        }

        Activity activity = activityService.getActivityOrThrow(registration.getActivityId());
        if (activity.getStartTime() != null && !activity.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("活动开始后不可取消报名");
        }
        removeById(registrationId);
        activity.setCurrentPeople(Math.max(0, activity.getCurrentPeople() - 1));
        activityService.updateById(activity);
        dashboardCacheService.evictDashboardCache();
        profileCacheService.evictProfileCache(currentUser.getId());
        activityCacheService.evictAllActivityCaches();
    }

    private void sendRegistrationCreatedMessage(Activity activity, SysUser currentUser) {
        ActivityMessage message = new ActivityMessage();
        message.setEventType("REGISTRATION_CREATED");
        message.setActivityId(activity.getId());
        message.setActivityTitle(activity.getTitle());
        message.setUserId(currentUser.getId());
        message.setUsername(currentUser.getUsername());
        message.setEventTime(LocalDateTime.now());
        message.setDescription("用户报名活动成功");
        activityMessageProducer.send(RabbitMqConfig.REGISTRATION_CREATED_ROUTING_KEY, message);
    }
}

