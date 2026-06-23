package com.campus.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String fileName;
    private String filePath;
    private Long fileSize;
    private Long uploadUserId;
    private LocalDateTime createTime;
}
