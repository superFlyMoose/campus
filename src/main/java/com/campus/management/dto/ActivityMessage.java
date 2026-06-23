package com.campus.management.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

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
