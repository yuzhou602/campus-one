package com.campusone.activity.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("campus_activity")
public class CampusActivity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String activityCode;
    private String title;
    private String description;
    private String category;
    private String coverImage;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationDeadline;
    private Integer capacity;
    private Integer registeredCount;
    private String organizer;
    private Long creatorId;
    private String status;
    private LocalDateTime createdAt;
}
