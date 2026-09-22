package com.campusone.activity.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusone.activity.entity.ActivityRegistration;
import com.campusone.activity.entity.CampusActivity;
import com.campusone.activity.service.ActivityService;
import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.common.exception.BusinessException;
import com.campusone.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "校园活动")
@RestController
@RequestMapping("/api/v1/activities")
@RequiredArgsConstructor
public class ActivityController {
    private final ActivityService activityService;

    @Operation(summary = "活动列表")
    @GetMapping
    public ApiResponse<PageResult<CampusActivity>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String category) {
        IPage<CampusActivity> result = activityService.listActivities(page, pageSize, category);
        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "活动详情")
    @GetMapping("/{id}")
    public ApiResponse<CampusActivity> getById(@PathVariable Long id) {
        CampusActivity activity = activityService.getActivityById(id);
        if (activity == null) throw new BusinessException("活动不存在");
        return ApiResponse.success(activity);
    }

    @Operation(summary = "报名活动")
    @PostMapping("/{id}/register")
    public ApiResponse<Void> register(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        activityService.registerActivity(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "我的活动")
    @GetMapping("/my")
    public ApiResponse<PageResult<CampusActivity>> myActivities(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        IPage<CampusActivity> result = activityService.getMyActivities(userId, page, pageSize);
        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "取消报名")
    @DeleteMapping("/{id}/register")
    public ApiResponse<Void> cancelRegistration(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        activityService.cancelRegistration(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "签到")
    @PostMapping("/{id}/checkin")
    public ApiResponse<Void> checkin(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        activityService.checkin(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "我的报名")
    @GetMapping("/registrations/my")
    public ApiResponse<?> myRegistrations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(activityService.getMyRegistrations(userId, page, pageSize));
    }
}
