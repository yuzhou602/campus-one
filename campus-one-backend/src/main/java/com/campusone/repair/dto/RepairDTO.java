package com.campusone.repair.dto;

import lombok.Data;

@Data
public class RepairDTO {
    private String description;
    private String location;
    private String category;
    private String contact;
    private String imageUrl;
    private String availableTime;
}
