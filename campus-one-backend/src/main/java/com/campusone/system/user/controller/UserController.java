package com.campusone.system.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.security.RequiresRole;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@RequiresRole("SUPER_ADMIN")
public class UserController {
    private final UserService userService;

    @Operation(summary = "用户列表")
    @GetMapping
    public ApiResponse<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        IPage<User> result = userService.listUsers(page, pageSize, keyword);
        return ApiResponse.success(PageResult.of(result));
    }
}
