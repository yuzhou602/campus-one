package com.campusone.security;

import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {
    @Mock
    private UserMapper userMapper;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recognizesSecurityUserRolesAndOwnership() {
        User user = new User();
        user.setId(7L);
        user.setUsername("admin");
        user.setPassword("encoded-password");
        user.setRole("ADMIN");
        SecurityUser principal = new SecurityUser(user, List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        PermissionService service = new PermissionService(userMapper);

        assertTrue(service.hasRole("ADMIN"));
        assertTrue(service.hasAnyRole("STUDENT", "ADMIN"));
        assertFalse(service.hasRole("SUPER_ADMIN"));
        assertTrue(service.isOwner(7L));
        assertFalse(service.isOwner(8L));
        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectsAnonymousAuthentication() {
        PermissionService service = new PermissionService(userMapper);

        assertFalse(service.hasRole("ADMIN"));
        assertFalse(service.hasPermission("system:user:read"));
        assertFalse(service.isOwner(1L));
    }
}
