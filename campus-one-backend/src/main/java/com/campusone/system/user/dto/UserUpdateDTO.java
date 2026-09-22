package com.campusone.system.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDTO {
    @Size(min = 2, max = 50, message = "真实姓名长度2-50")
    private String realName;
    private String phone;
    private String email;
    private String avatar;
    private String bio;
}
