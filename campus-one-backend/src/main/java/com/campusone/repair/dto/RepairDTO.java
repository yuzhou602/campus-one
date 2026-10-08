package com.campusone.repair.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RepairDTO {
    @NotBlank(message = "故障描述不能为空")
    @Size(max = 1000, message = "故障描述不能超过1000个字符")
    private String description;
    @NotBlank(message = "报修地点不能为空")
    @Size(max = 200, message = "报修地点不能超过200个字符")
    private String location;
    @NotBlank(message = "报修分类不能为空")
    private String category;
    @NotBlank(message = "联系方式不能为空")
    @Size(max = 100, message = "联系方式不能超过100个字符")
    private String contact;
    @Size(max = 1000, message = "图片地址不能超过1000个字符")
    private String imageUrl;
    @Size(max = 200, message = "可上门时间不能超过200个字符")
    private String availableTime;
}
