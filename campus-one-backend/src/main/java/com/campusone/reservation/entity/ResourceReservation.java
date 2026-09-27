package com.campusone.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("resource_reservation")
public class ResourceReservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String reservationNo;
    private Long resourceId;
    @TableField(exist = false)
    private String resourceName;
    private Long userId;
    @TableField(exist = false)
    private String userName;
    private LocalDate reservationDate;
    private String startTime;
    private String endTime;
    private String purpose;
    private Integer participantCount;
    private String status;
    private String approvalInstanceId;
    private Long approvedBy;
    private String approvalRemark;
    private LocalDateTime approvedAt;
    @Version
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
