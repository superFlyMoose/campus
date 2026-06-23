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

    @RabbitListener(queues = RabbitMqConfig.ACTIVITY_QUEUE)
    public void onMessage(ActivityMessage message) {
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
