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

    public void send(String routingKey, ActivityMessage message) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.ACTIVITY_EXCHANGE, routingKey, message);
    }
}
