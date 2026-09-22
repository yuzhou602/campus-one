package com.campusone.application.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("application_approval_record")
public class ApprovalRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private String taskId;
    private String nodeName;
    private Long assigneeId;
    private String assigneeName;
    private String action;
    private String comment;
    private LocalDateTime createdAt;
}
