package com.campusone.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("campus_resource")
public class CampusResource implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String resourceCode;
    private String resourceName;
    private String resourceType;
    private Long buildingId;
    private String roomNumber;
    private Integer capacity;
    private String description;
    private String equipmentJson;
    private String status;
    private Boolean needApproval;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
