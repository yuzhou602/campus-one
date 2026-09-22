package com.campusone.system.user.dto;

import com.campusone.system.user.vo.UserVO;
import lombok.Data;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserVO userInfo;
}
