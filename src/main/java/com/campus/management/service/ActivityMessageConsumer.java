package com.campus.management.service;

import com.campus.management.config.RabbitMqConfig;
import com.campus.management.dto.ActivityMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ActivityMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivityMessageConsumer.class);

    /**
     * RabbitMQ 消费者
     * 监听活动业务消息队列，处理异步事件
     */
    @RabbitListener(queues = RabbitMqConfig.ACTIVITY_QUEUE)
    public void onMessage(ActivityMessage message) {
        // 记录消费日志：用于追踪消息流转与问题排查
        log.info("RabbitMQ消息消费成功: eventType={}, activityId={}, activityTitle={}, userId={}, username={}, eventTime={}, description={}",
            message.getEventType(),
            message.getActivityId(),
            message.getActivityTitle(),
            message.getUserId(),
            message.getUsername(),
            message.getEventTime(),
            message.getDescription());
    }
}
