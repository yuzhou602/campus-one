package com.campusone.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.security.JwtUtil;
import com.campusone.security.TokenBlacklist;
import com.campusone.security.TokenBlacklistMapper;
import com.campusone.system.user.dto.LoginRequest;
import com.campusone.system.user.dto.LoginResponse;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import com.campusone.system.user.service.UserService;
import com.campusone.system.user.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistMapper tokenBlacklistMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = this.baseMapper.selectOne(wrapper);
        if (user == null) {
            throw new BusinessException(401, "用户名不存在");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(token);
        response.setRefreshToken(refresh);
        response.setExpiresIn(jwtUtil.getExpiration() / 1000);
        response.setUserInfo(toUserVO(user, loadPermissions(user)));
        return response;
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVO(user, loadPermissions(user));
    }

    @Override
    public IPage<User> listUsers(int page, int size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword)
                    .or().like(User::getRealName, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        blacklistToken(accessToken);
        blacklistToken(refreshToken);
    }

    private void blacklistToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token != null && jwtUtil.validateToken(token)) {
            TokenBlacklist blacklisted = new TokenBlacklist();
            blacklisted.setToken(token);
            try {
                Long userId = jwtUtil.getUserId(token);
                blacklisted.setUserId(userId);
                Date expiration = jwtUtil.getExpirationFromToken(token);
                blacklisted.setExpiresAt(expiration.toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            } catch (Exception e) {
                blacklisted.setExpiresAt(LocalDateTime.now().plusDays(1));
            }
            try {
                tokenBlacklistMapper.insert(blacklisted);
            } catch (DuplicateKeyException ignored) {
                // Idempotent logout: an already revoked token stays revoked.
            }
        }
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtUtil.validateToken(refreshToken)
                || !jwtUtil.isRefreshToken(refreshToken)
                || isBlacklisted(refreshToken)) {
            throw new BusinessException("无效的Refresh Token");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        User user = this.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户不存在或已被禁用");
        }

        TokenBlacklist oldToken = new TokenBlacklist();
        oldToken.setToken(refreshToken);
        oldToken.setUserId(userId);
        Date expiration = jwtUtil.getExpirationFromToken(refreshToken);
        oldToken.setExpiresAt(expiration.toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDateTime());
        tokenBlacklistMapper.insert(oldToken);

        String newAccess = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        String newRefresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccess);
        response.setRefreshToken(newRefresh);
        response.setExpiresIn(jwtUtil.getExpiration() / 1000);
        response.setUserInfo(toUserVO(user, loadPermissions(user)));
        return response;
    }

    private List<String> loadPermissions(User user) {
        if ("SUPER_ADMIN".equals(user.getRole())) {
            return List.of("*");
        }
        List<String> permissions = this.baseMapper.getUserPermissions(user.getId());
        return permissions == null ? List.of() : List.copyOf(permissions);
    }

    private boolean isBlacklisted(String token) {
        return tokenBlacklistMapper.selectCount(
                new LambdaQueryWrapper<TokenBlacklist>().eq(TokenBlacklist::getToken, token)) > 0;
    }

    private UserVO toUserVO(User user, List<String> permissions) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setRoleName(getRoleName(user.getRole()));
        vo.setPermissions(permissions);
        vo.setDataScope(user.getDataScope());
        vo.setCollegeId(user.getCollegeId());
        vo.setMajorId(user.getMajorId());
        vo.setClassId(user.getClassId());
        return vo;
    }

    private String getRoleName(String role) {
        return switch (role) {
            case "SUPER_ADMIN" -> "超级管理员";
            case "ADMIN" -> "管理员";
            case "TEACHER" -> "教师";
            case "COUNSELOR" -> "辅导员";
            case "STUDENT" -> "学生";
            case "SERVICE" -> "服务人员";
            default -> role;
        };
    }
}
