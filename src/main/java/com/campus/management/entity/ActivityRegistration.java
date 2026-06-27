package com.campus.management.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("activity_registration")
public class ActivityRegistration {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long activityId;
    @TableField(exist = false)
    private String activityTitle;
    private Long userId;
    private LocalDateTime registrationTime;
    @TableLogic
    private Integer isDeleted;
}
