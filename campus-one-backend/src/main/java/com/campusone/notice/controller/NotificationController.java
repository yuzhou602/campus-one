package com.campusone.notice.controller;

import com.campusone.common.response.ApiResponse;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.service.impl.NoticeServiceImpl;
import com.campusone.security.UserContext;
import com.campusone.security.RequiresRole;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "通知(前端兼容)")
@Hidden
@Deprecated(forRemoval = false)
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NoticeServiceImpl noticeService;

    @Operation(summary = "我的通知列表")
    @GetMapping("/my")
    public ApiResponse<List<Notification>> myNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(noticeService.getMyNotices(userId, page, pageSize, null).getRecords());
    }

    @Operation(summary = "未读通知数")
    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount() {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success((long) noticeService.getUnreadCount(userId));
    }

    @Operation(summary = "标记已读")
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        noticeService.markAsRead(id, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead() {
        Long userId = UserContext.getCurrentUserId();
        noticeService.markAllAsRead(userId);
        return ApiResponse.success();
    }

    @Operation(summary = "通知详情")
    @GetMapping("/{id}")
    public ApiResponse<Notification> getById(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        return ApiResponse.success(noticeService.getVisibleNotice(id, userId));
    }

    @Operation(summary = "删除通知")
    @DeleteMapping("/{id}")
    @RequiresRole({"COUNSELOR", "ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return ApiResponse.success();
    }
}
