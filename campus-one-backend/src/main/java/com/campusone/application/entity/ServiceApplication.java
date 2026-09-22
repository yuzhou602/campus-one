package com.campusone.application.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("service_application")
public class ServiceApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applicationNo;
    private Long serviceId;
    private Long applicantId;
    private String formDataJson;
    private String status;
    private String processInstanceId;
    private String currentNode;
    private Integer urgeCount;
    private LocalDateTime lastUrgedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
