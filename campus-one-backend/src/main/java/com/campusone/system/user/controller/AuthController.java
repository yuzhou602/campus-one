package com.campusone.system.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusone.common.response.ApiResponse;
import com.campusone.security.UserContext;
import com.campusone.system.user.dto.LoginRequest;
import com.campusone.system.user.dto.LoginResponse;
import com.campusone.system.user.dto.RegisterRequest;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.service.UserService;
import com.campusone.system.user.vo.UserVO;
import com.campusone.common.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        User existing = userService.getOne(
            new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        // Public registration can never grant privileged roles.
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setSchoolId(request.getSchoolId());
        user.setCollegeId(request.getCollegeId());
        user.setMajorId(request.getMajorId());
        user.setClassId(request.getClassId());
        userService.save(user);
        return ApiResponse.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/userinfo")
    public ApiResponse<UserVO> getUserInfo() {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(userService.getCurrentUser(userId));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public ApiResponse<User> getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return ApiResponse.success(user);
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request,
                                    @RequestBody(required = false) Map<String, String> body) {
        String accessToken = request.getHeader("Authorization");
        if (body != null && body.get("accessToken") != null) {
            accessToken = body.get("accessToken");
        }
        String refreshToken = body == null ? null : body.get("refreshToken");
        userService.logout(accessToken, refreshToken);
        return ApiResponse.success();
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        return ApiResponse.success(userService.refresh(refreshToken));
    }
}
