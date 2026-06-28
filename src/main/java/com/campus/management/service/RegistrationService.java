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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    /**
     * 获取当前用户所有报名记录
     */
    public List<ActivityRegistration> getMyRegistrations(Long userId) {
        List<ActivityRegistration> registrations = lambdaQuery()
            .eq(ActivityRegistration::getUserId, userId)
            .eq(ActivityRegistration::getIsDeleted, 0)
            .orderByDesc(ActivityRegistration::getRegistrationTime)
            .list();
        fillActivityTitles(registrations);
        return registrations;
    }

    /**
     * 用户报名记录分页查询
     */
    public Page<ActivityRegistration> pageMyRegistrations(Long userId, int pageNum, int pageSize) {
        Page<ActivityRegistration> page = new Page<>(Math.max(pageNum, 1), pageSize);
        Page<ActivityRegistration> registrationPage = lambdaQuery()
            .eq(ActivityRegistration::getUserId, userId)
            .eq(ActivityRegistration::getIsDeleted, 0)
            .orderByDesc(ActivityRegistration::getRegistrationTime)
            .page(page);
        fillActivityTitles(registrationPage.getRecords());
        return registrationPage;
    }

    /**
     * 用户报名活动
     */
    @Transactional
    public void register(Long activityId, SysUser currentUser) {
        Activity activity = activityService.getActivityOrThrow(activityId);
        boolean exists = hasRegistered(activityId, currentUser.getId());
        if (exists) {
            throw new IllegalArgumentException("已报名，不可重复报名！");
        }
        if (activity.getStartTime() != null && !activity.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("活动已开始，无法报名！");
        }
        if (!activityService.canRegister(activity)) {
            throw new IllegalArgumentException("当前活动无法报名");
        }
        // 创建报名记录
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(currentUser.getId());
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setIsDeleted(0);
        save(registration);
        // 更新活动人数
        activity.setCurrentPeople(activity.getCurrentPeople() + 1);
        activityService.updateById(activity);
        dashboardCacheService.evictDashboardCache();
        // 缓存失效
        profileCacheService.evictProfileCache(currentUser.getId());
        activityCacheService.evictAllActivityCaches();
        // 异步事件通知
        sendRegistrationCreatedMessage(activity, currentUser);
    }

    /**
     * 用户取消报名
     */
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
        // 删除报名记录
        removeById(registrationId);
        // 回滚人数
        activity.setCurrentPeople(Math.max(0, activity.getCurrentPeople() - 1));
        activityService.updateById(activity);
        // 缓存失效
        dashboardCacheService.evictDashboardCache();
        profileCacheService.evictProfileCache(currentUser.getId());
        activityCacheService.evictAllActivityCaches();
    }

    /**
     * 判断用户是否已报名某活动
     */
    public boolean hasRegistered(Long activityId, Long userId) {
        return lambdaQuery()
            .eq(ActivityRegistration::getActivityId, activityId)
            .eq(ActivityRegistration::getUserId, userId)
            .eq(ActivityRegistration::getIsDeleted, 0)
            .count() > 0;
    }

    /**
     * 补充报名记录中的活动标题，避免前端二次查询
     */
    private void fillActivityTitles(List<ActivityRegistration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            return;
        }
        Set<Long> activityIds = registrations.stream()
            .map(ActivityRegistration::getActivityId)
            .filter(activityId -> activityId != null)
            .collect(Collectors.toSet());
        if (activityIds.isEmpty()) {
            return;
        }
        Map<Long, String> activityTitleMap = activityService.listByIds(activityIds).stream()
            .filter(activity -> !Integer.valueOf(1).equals(activity.getIsDeleted()))
            .collect(Collectors.toMap(Activity::getId, Activity::getTitle, (left, right) -> left));
        registrations.forEach(registration -> registration.setActivityTitle(
            activityTitleMap.getOrDefault(registration.getActivityId(), "活动已删除")
        ));
    }

    /**
     * 发送“报名成功”事件消息
     */
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
