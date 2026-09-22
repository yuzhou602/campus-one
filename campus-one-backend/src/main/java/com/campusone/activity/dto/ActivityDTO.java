package com.campusone.activity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ActivityDTO {
    @NotBlank(message = "活动标题不能为空")
    private String title;
    private String description;
    private String category;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer capacity;
    private String imageUrl;
}
