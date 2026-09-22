package com.campusone.application.dto;

import lombok.Data;

@Data
public class ApplicationDTO {
    private Long serviceId;
    private String title;
    private String content;
    private String formData;
}
