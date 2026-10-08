package com.campusone.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationDTO {
    @NotNull(message = "服务事项不能为空")
    private Long serviceId;
    @Size(max = 100, message = "申请标题不能超过100字")
    private String title;
    @Size(max = 2000, message = "申请内容不能超过2000字")
    private String content;
    @NotBlank(message = "申请表单不能为空")
    @Size(max = 10000, message = "申请表单数据不能超过10000字")
    private String formData;
}
