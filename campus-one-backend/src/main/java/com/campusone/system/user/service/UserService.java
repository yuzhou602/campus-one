package com.campusone.system.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.system.user.dto.LoginRequest;
import com.campusone.system.user.dto.LoginResponse;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.vo.UserVO;

public interface UserService extends IService<User> {
    LoginResponse login(LoginRequest request);
    UserVO getCurrentUser(Long userId);
    IPage<User> listUsers(int page, int size, String keyword);
    void logout(String accessToken, String refreshToken);
    LoginResponse refresh(String refreshToken);
}
