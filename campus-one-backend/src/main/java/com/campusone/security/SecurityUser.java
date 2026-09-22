package com.campusone.security;

import com.campusone.system.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class SecurityUser extends org.springframework.security.core.userdetails.User {
    private final Long userId;
    private final String realName;
    private final String avatar;
    private final String role;
    private final Long schoolId;

    public SecurityUser(User user, List<String> roles) {
        super(user.getUsername(), user.getPassword(),
                user.getStatus() != null && user.getStatus() == 1,
                true, true, true,
                roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList()));
        this.userId = user.getId();
        this.realName = user.getRealName();
        this.avatar = user.getAvatar();
        this.role = user.getRole();
        this.schoolId = user.getSchoolId();
    }
}
