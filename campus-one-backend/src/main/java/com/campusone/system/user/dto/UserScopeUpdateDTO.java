package com.campusone.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserScopeUpdateDTO {
    @NotBlank(message = "数据范围不能为空")
    @Pattern(regexp = "ALL|COLLEGE|CLASS|SELF", message = "数据范围不合法")
    private String dataScope;
    private Long collegeId;
    private Long classId;
}
