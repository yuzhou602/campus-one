package com.campusone.system.user.vo;

import lombok.Data;
import java.util.List;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String realName;
    private String avatar;
    private String email;
    private String phone;
    private String role;
    private String roleName;
    private List<String> permissions;
    private String dataScope;
    private Long collegeId;
    private String collegeName;
    private Long majorId;
    private String majorName;
    private Long classId;
    private String className;
}
