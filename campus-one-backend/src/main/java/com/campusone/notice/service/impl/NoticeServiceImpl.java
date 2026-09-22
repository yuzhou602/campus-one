package com.campusone.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.mapper.NotificationMapper;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.notice.service.NoticeService;
import com.campusone.notice.service.NotificationDistributionService;
import com.campusone.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NoticeService {

    private final UserNotificationMapper userNotificationMapper;
    private final NotificationDistributionService notificationDistributionService;

    public Notification create(Notification notification) {
        this.save(notification);
        notificationDistributionService.distributeAsync(
                notification.getId(), notification.getTargetType(), notification.getTargetId());
        return notification;
    }

    @Override
    public IPage<Notification> getMyNotices(Long userId, int page, int size, String type) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w
            .eq(Notification::getTargetType, "ALL")
            .or().eq(Notification::getTargetType, "USER").eq(Notification::getTargetId, userId));
        if (type != null && !"all".equals(type)) {
            wrapper.eq(Notification::getType, type);
        }
        wrapper.orderByDesc(Notification::getCreatedAt);
        return this.page(new Page<>(page, size), wrapper);
    }

    @Override
    public int getUnreadCount(Long userId) {
        return (int) userNotificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        userNotificationMapper.markAsRead(id, userId);
    }

    public void updateNotice(Long id, Notification notification) {
        Notification existing = this.getById(id);
        if (existing == null) {
            throw new BusinessException("通知不存在");
        }
        String role = UserContext.getCurrentUserRole();
        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role) && !"COUNSELOR".equals(role)) {
            throw new BusinessException(403, "无权修改通知");
        }
        notification.setId(id);
        this.updateById(notification);
    }
}
