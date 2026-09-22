package com.campusone.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeDTO {
    @NotBlank(message = "标题不能为空")
    @Size(max = 120, message = "标题不能超过120字")
    private String title;
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容不能超过10000字")
    private String content;
    @NotBlank(message = "通知范围不能为空")
    @Pattern(regexp = "ALL|USER", message = "通知范围仅支持 ALL 或 USER")
    private String targetType;
    private Long targetId;
    private String priority;
    private String category;
}
