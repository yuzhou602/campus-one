package com.campusone.notice.controller;

import com.campusone.common.response.ApiResponse;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.NotificationMapper;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.security.UserContext;
import com.campusone.security.RequiresRole;
import com.campusone.common.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "通知(前端兼容)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationMapper notificationMapper;
    private final UserNotificationMapper userNotificationMapper;

    @Operation(summary = "我的通知列表")
    @GetMapping("/my")
    public ApiResponse<List<Notification>> myNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        List<UserNotification> unList = userNotificationMapper.selectList(
            new LambdaQueryWrapper<UserNotification>()
                .eq(UserNotification::getUserId, userId)
                .orderByDesc(UserNotification::getCreatedAt)
                .last("LIMIT " + safePageSize + " OFFSET " + (long) (safePage - 1) * safePageSize));
        List<Notification> notices = new ArrayList<>();
        for (UserNotification un : unList) {
            Notification n = notificationMapper.selectById(un.getNotificationId());
            if (n != null) {
                notices.add(n);
            }
        }
        return ApiResponse.success(notices);
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(userNotificationMapper.countUnread(userId));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        userNotificationMapper.markAsRead(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = UserContext.getCurrentUserId();
        userNotificationMapper.markAllAsRead(userId);
        return ApiResponse.success();
    }

    @Operation(summary = "通知详情")
    @GetMapping("/{id}")
    public ApiResponse<Notification> getById(@PathVariable Long id) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null) throw new BusinessException("通知不存在");
        Long userId = UserContext.getCurrentUserId();
        boolean visible = "ALL".equals(notification.getTargetType())
                || ("USER".equals(notification.getTargetType()) && userId.equals(notification.getTargetId()))
                || userNotificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>()
                    .eq(UserNotification::getNotificationId, id)
                    .eq(UserNotification::getUserId, userId)) > 0;
        if (!visible) throw new BusinessException(403, "无权查看该通知");
        return ApiResponse.success(notification);
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    @RequiresRole({"COUNSELOR", "ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        notificationMapper.deleteById(id);
        return ApiResponse.success();
    }
}
