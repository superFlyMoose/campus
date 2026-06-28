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

    /**
     * 条件分页查询活动列表
     *
     * @param keyword  活动标题关键字（模糊匹配）
     * @param location 活动地点（模糊匹配）
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @return 分页活动数据
     */
    public Page<Activity> searchActivities(String keyword, String location, int pageNum, int pageSize) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        // 只查询未删除数据
        wrapper.eq(Activity::getIsDeleted, 0)
            .like(StringUtils.hasText(keyword), Activity::getTitle, keyword)
            .like(StringUtils.hasText(location), Activity::getLocation, location)
            .orderByDesc(Activity::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 获取最新活动列表（用于首页展示）
     *
     * @param limit 返回数量限制
     */
    public List<Activity> listLatestActivities(int limit) {
        return lambdaQuery()
            .eq(Activity::getIsDeleted, 0)
            .orderByDesc(Activity::getStartTime)
            .last("limit " + limit)
            .list();
    }

    /**
     * 统计当前有效活动总数
     */
    public long countActiveActivities() {
        return lambdaQuery().eq(Activity::getIsDeleted, 0).count();
    }

    /**
     * 根据ID获取活动，若不存在或已删除则抛出异常
     *
     * @param id 活动ID
     * @return 活动实体
     */
    public Activity getActivityOrThrow(Long id) {
        Activity activity = getById(id);
        // 防止逻辑删除数据被误用
        if (activity == null || Integer.valueOf(1).equals(activity.getIsDeleted())) {
            throw new IllegalArgumentException("活动不存在");
        }
        return activity;
    }

    /**
     * 创建活动
     */
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
        // 缓存失效，保证一致性
        dashboardCacheService.evictDashboardCache();
        activityCacheService.evictAllActivityCaches();
        sendActivityCreatedMessage(activity);  // 发送活动创建消息
    }

    /**
     * 更新活动信息
     */
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

    /**
     * 删除活动
     */
    public void removeActivity(Long id) {
        removeById(id);
        dashboardCacheService.evictDashboardCache();
        activityCacheService.evictAllActivityCaches();
    }

    /**
     * 判断活动是否允许报名
     *
     * 条件：
     * 1.活动已开始时间必须在当前时间之后
     * 2.当前报名人数未超过最大人数限制
     */
    public boolean canRegister(Activity activity) {
        return activity.getStartTime() != null
            && activity.getStartTime().isAfter(LocalDateTime.now())
            && activity.getCurrentPeople() < activity.getMaxPeople();
    }

    /**
     * 绑定活动封面图片
     *
     * @param form 活动表单
     * @param imageFile 上传文件
     */
    public void bindActivityImage(ActivityForm form, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return;
        }
        // 文件上传并保存路径
        form.setImagePath(fileService.saveFile(imageFile, 0L, "activity").getFilePath());
    }

    /**
     * 校验活动时间合法性
     */
    private void validateTime(ActivityForm form) {
        if (form.getStartTime() != null && form.getEndTime() != null
            && !form.getEndTime().isAfter(form.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }
    }

    /**
     * 发送“活动创建成功”事件消息
     */
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


