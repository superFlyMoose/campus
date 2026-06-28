package com.campus.management.service;

import com.campus.management.config.RabbitMqConfig;
import com.campus.management.dto.ActivityMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActivityMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public ActivityMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送活动业务消息到RabbitMQ
     *
     * @param routingKey 路由键，用于交换机路由到具体队列
     * @param message    活动事件消息体
     */
    public void send(String routingKey, ActivityMessage message) {
        // 发送消息到指定交换机，通过routingKey进行路由
        rabbitTemplate.convertAndSend(RabbitMqConfig.ACTIVITY_EXCHANGE, routingKey, message);
    }
}
