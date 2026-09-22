package com.campusone.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String repairNo;
    private Long userId;
    private String location;
    private String category;
    private String description;
    private String priority;
    private String status;
    private Long assignedUserId;
    private String contact;
    private String availableTime;
    private String aiCategory;
    private String aiPriority;
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
}
