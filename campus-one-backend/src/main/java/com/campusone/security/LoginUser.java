package com.campusone.security;

import lombok.Data;

@Data
public class LoginUser {
    private Long id;
    private String username;
    private String realName;
    private String role;
    private Long schoolId;

    public static LoginUser from(SecurityUser user) {
        LoginUser login = new LoginUser();
        login.setId(user.getUserId());
        login.setUsername(user.getUsername());
        login.setRealName(user.getRealName());
        login.setRole(user.getRole());
        login.setSchoolId(user.getSchoolId());
        return login;
    }
}
