package com.campusone.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    private String type;
    private String targetType;
    private Long targetId;
    private Long senderId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
