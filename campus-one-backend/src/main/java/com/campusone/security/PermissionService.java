package com.campusone.security;

import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final UserMapper userMapper;

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            if (role.equals(securityUser.getRole())) return true;
            return auth.getAuthorities().stream()
                    .anyMatch(authority -> ("ROLE_" + role).equals(authority.getAuthority()));
        }
        if (principal instanceof Long userId) {
            User user = userMapper.selectById(userId);
            if (user == null) return false;
            if (role.equals(user.getRole())) return true;
            return userMapper.hasUserRole(userId, role) > 0;
        }
        return false;
    }

    public boolean hasAnyRole(String... roles) {
        return roles != null && Arrays.stream(roles).anyMatch(this::hasRole);
    }

    public boolean hasPermission(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return userMapper.hasPermission(securityUser.getUserId(), permission) > 0;
        }
        if (principal instanceof Long userId) {
            return userMapper.hasPermission(userId, permission) > 0;
        }
        return false;
    }

    public boolean isOwner(Long resourceUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof SecurityUser securityUser) {
            return securityUser.getUserId().equals(resourceUserId);
        }
        if (principal instanceof Long userId) {
            return userId.equals(resourceUserId);
        }
        return false;
    }
}
