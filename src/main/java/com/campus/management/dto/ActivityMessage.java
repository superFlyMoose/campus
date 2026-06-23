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

//    public String getEventType() {
//        return eventType;
//    }
//
//    public void setEventType(String eventType) {
//        this.eventType = eventType;
//    }
//
//    public Long getActivityId() {
//        return activityId;
//    }
//
//    public void setActivityId(Long activityId) {
//        this.activityId = activityId;
//    }
//
//    public String getActivityTitle() {
//        return activityTitle;
//    }
//
//    public void setActivityTitle(String activityTitle) {
//        this.activityTitle = activityTitle;
//    }
//
//    public Long getUserId() {
//        return userId;
//    }
//
//    public void setUserId(Long userId) {
//        this.userId = userId;
//    }
//
//    public String getUsername() {
//        return username;
//    }
//
//    public void setUsername(String username) {
//        this.username = username;
//    }
//
//    public LocalDateTime getEventTime() {
//        return eventTime;
//    }
//
//    public void setEventTime(LocalDateTime eventTime) {
//        this.eventTime = eventTime;
//    }
//
//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }
}
