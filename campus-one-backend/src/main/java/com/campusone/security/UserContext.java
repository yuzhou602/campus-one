package com.campusone.security;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserContext {
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new InsufficientAuthenticationException("未登录");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long userId) {
            return userId;
        }
        if (principal instanceof SecurityUser securityUser) {
            return securityUser.getUserId();
        }
        throw new InsufficientAuthenticationException("无效的认证信息");
    }

    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new InsufficientAuthenticationException("未登录");
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElseThrow(() -> new InsufficientAuthenticationException("无角色信息"));
    }

    public static boolean isAdmin() {
        try {
            String role = getCurrentUserRole();
            return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        } catch (Exception e) {
            return false;
        }
    }
}
