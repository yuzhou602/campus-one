package com.campusone.notice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.notice.dto.NoticeDTO;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.notice.service.impl.NoticeServiceImpl;
import com.campusone.security.UserContext;
import com.campusone.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "通知")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {
    private final NoticeServiceImpl noticeService;
    private final UserNotificationMapper userNotificationMapper;

    @Operation(summary = "通知列表")
    @GetMapping
    public ApiResponse<PageResult<Notification>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String type) {
        Long userId = UserContext.getCurrentUserId();
        IPage<Notification> result = noticeService.getMyNotices(userId, page, pageSize, type);
        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "发布通知")
    @PostMapping
    @RequiresRole({"COUNSELOR", "ADMIN", "SUPER_ADMIN"})
    public ApiResponse<Notification> create(@Valid @RequestBody NoticeDTO dto) {
        if ("USER".equals(dto.getTargetType()) && dto.getTargetId() == null) {
            throw new com.campusone.common.exception.BusinessException("指定用户通知必须提供用户ID");
        }
        Notification notice = new Notification();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setTargetType(dto.getTargetType());
        notice.setTargetId(dto.getTargetId());
        notice.setSenderId(UserContext.getCurrentUserId());
        noticeService.create(notice);
        return ApiResponse.success(notice);
    }

    @Operation(summary = "通知详情")
    @GetMapping("/{id}")
    public ApiResponse<Notification> getById(@PathVariable Long id) {
        Notification notification = noticeService.getById(id);
        Long userId = UserContext.getCurrentUserId();
        if (notification == null) {
            throw new com.campusone.common.exception.BusinessException("通知不存在");
        }
        if ("USER".equals(notification.getTargetType()) && !userId.equals(notification.getTargetId())) {
            throw new com.campusone.common.exception.BusinessException(403, "无权访问");
        }
        return ApiResponse.success(notification);
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
}
