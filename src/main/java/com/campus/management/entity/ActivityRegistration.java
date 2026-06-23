package com.campus.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@TableName("activity_registration")
public class ActivityRegistration {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long activityId;
    private Long userId;
    private LocalDateTime registrationTime;

    @TableLogic
    private Integer isDeleted;
}
