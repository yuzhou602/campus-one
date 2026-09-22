package com.campusone.notice.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long notificationId;
    private Long userId;
    private Boolean isRead;
    private LocalDateTime readAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
