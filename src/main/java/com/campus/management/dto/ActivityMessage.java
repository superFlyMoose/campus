package com.campus.management.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动事件消息对象
 * 用于系统内部异步通信，
 * 表示与活动相关的业务事件，用于解耦业务流程
 */
@Data
public class ActivityMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventType;
    private Long activityId;
    private String activityTitle;
    private Long userId;
    private String username;
    private LocalDateTime eventTime;
    private String description;
}
