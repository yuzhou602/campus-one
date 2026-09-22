package com.campusone.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusone.common.exception.BusinessException;
import com.campusone.security.JwtUtil;
import com.campusone.security.TokenBlacklist;
import com.campusone.security.TokenBlacklistMapper;
import com.campusone.system.user.dto.LoginRequest;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import com.campusone.system.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenBlacklistMapper tokenBlacklistMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUpBaseMapper() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("$2a$10$encoded");
        testUser.setRealName("管理员");
        testUser.setRole("ADMIN");
        testUser.setStatus(1);
    }

    @Test
    @DisplayName("登录 - 凭证正确返回token")
    void login_shouldReturnTokenWhenCredentialsValid() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("refresh-token");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);
        when(userMapper.getUserPermissions(1L)).thenReturn(List.of("reservation:approve", "repair:assign"));

        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername("admin");
        loginReq.setPassword("admin123");
        var result = userService.login(loginReq);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
        assertEquals(86400, result.getExpiresIn());
        assertNotNull(result.getUserInfo());
        assertEquals("ADMIN", result.getUserInfo().getRole());
        assertEquals(List.of("reservation:approve", "repair:assign"), result.getUserInfo().getPermissions());
    }

    @Test
    @DisplayName("登录 - 用户不存在")
    void login_shouldThrowWhenUserNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsername("nonexistent");
            loginReq.setPassword("password");
            userService.login(loginReq);
        });
        assertEquals(401, ex.getCode());
        assertEquals("用户名不存在", ex.getMessage());
    }

    @Test
    @DisplayName("登录 - 密码错误")
    void login_shouldThrowWhenPasswordWrong() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsername("admin");
            loginReq.setPassword("wrongpassword");
            userService.login(loginReq);
        });
        assertEquals(401, ex.getCode());
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("登录 - 账号已禁用")
    void login_shouldThrowWhenUserDisabled() {
        testUser.setStatus(0);
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsername("admin");
            loginReq.setPassword("admin123");
            userService.login(loginReq);
        });
        assertEquals(403, ex.getCode());
        assertEquals("账号已被禁用", ex.getMessage());
    }

    @Test
    @DisplayName("登出 - 正常注销")
    void logout_shouldBlacklistToken() {
        when(jwtUtil.validateToken("test-token")).thenReturn(true);
        when(jwtUtil.getUserId("test-token")).thenReturn(1L);
        when(jwtUtil.getExpirationFromToken("test-token")).thenReturn(new Date(System.currentTimeMillis() + 86400000L));
        when(tokenBlacklistMapper.insert((TokenBlacklist) any())).thenReturn(1);

        userService.logout("Bearer test-token", null);

        verify(tokenBlacklistMapper).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("当前用户 - 超级管理员保留通配权限")
    void getCurrentUser_shouldOnlyGiveWildcardToSuperAdmin() {
        testUser.setRole("SUPER_ADMIN");
        when(userMapper.selectById(1L)).thenReturn(testUser);

        var result = userService.getCurrentUser(1L);

        assertEquals(List.of("*"), result.getPermissions());
        verify(userMapper, never()).getUserPermissions(anyLong());
    }

    @Test
    @DisplayName("登出 - 无效token不报错")
    void logout_shouldNotThrowOnInvalidToken() {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        assertDoesNotThrow(() -> userService.logout("Bearer invalid-token", null));
        verify(tokenBlacklistMapper, never()).insert((TokenBlacklist) any());
    }

    @Test
    @DisplayName("登出 - null token不报错")
    void logout_shouldNotThrowOnNullToken() {
        assertDoesNotThrow(() -> userService.logout(null, null));
        verify(tokenBlacklistMapper, never()).insert((TokenBlacklist) any());
    }

    @Test
    @DisplayName("登出 - 无Bearer前缀也能处理")
    void logout_shouldHandleTokenWithoutBearerPrefix() {
        when(jwtUtil.validateToken("test-token")).thenReturn(true);
        when(jwtUtil.getUserId("test-token")).thenReturn(1L);
        when(jwtUtil.getExpirationFromToken("test-token")).thenReturn(new Date(System.currentTimeMillis() + 86400000L));
        when(tokenBlacklistMapper.insert((TokenBlacklist) any())).thenReturn(1);

        userService.logout("test-token", null);

        verify(tokenBlacklistMapper).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("登出 - 同时吊销访问令牌和刷新令牌")
    void logout_shouldBlacklistBothTokens() {
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUserId(anyString())).thenReturn(1L);
        when(jwtUtil.getExpirationFromToken(anyString()))
                .thenReturn(new Date(System.currentTimeMillis() + 86400000L));
        when(tokenBlacklistMapper.insert(any(TokenBlacklist.class))).thenReturn(1);

        userService.logout("access-token", "refresh-token");

        verify(tokenBlacklistMapper, times(2)).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("刷新token - 有效refresh token")
    void refresh_shouldReturnNewTokens() {
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getUserId("refresh-token")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(testUser);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(any(), any(), any())).thenReturn("new-refresh");
        when(jwtUtil.getExpiration()).thenReturn(86400000L);
        when(jwtUtil.getExpirationFromToken("refresh-token")).thenReturn(new Date(System.currentTimeMillis() + 604800000L));
        when(tokenBlacklistMapper.insert((TokenBlacklist) any())).thenReturn(1);
        when(userMapper.getUserPermissions(1L)).thenReturn(List.of("reservation:approve"));

        var result = userService.refresh("refresh-token");

        assertNotNull(result);
        assertEquals("new-access", result.getAccessToken());
        assertEquals("new-refresh", result.getRefreshToken());
        assertEquals(List.of("reservation:approve"), result.getUserInfo().getPermissions());
        verify(tokenBlacklistMapper).insert(any(TokenBlacklist.class));
    }

    @Test
    @DisplayName("刷新token - 无效token")
    void refresh_shouldThrowOnInvalidToken() {
        when(jwtUtil.validateToken("invalid")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.refresh("invalid"));
    }

    @Test
    @DisplayName("刷新token - 非refresh类型token")
    void refresh_shouldThrowWhenNotRefreshToken() {
        when(jwtUtil.validateToken("access-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("access-token")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.refresh("access-token"));
    }

    @Test
    @DisplayName("刷新token - 用户不存在")
    void refresh_shouldThrowWhenUserNotFound() {
        when(jwtUtil.validateToken("refresh-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtUtil.getUserId("refresh-token")).thenReturn(999L);
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.refresh("refresh-token"));
    }
}
