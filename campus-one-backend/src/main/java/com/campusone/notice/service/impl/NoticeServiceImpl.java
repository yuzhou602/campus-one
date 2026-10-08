package com.campusone.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.NotificationMapper;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.notice.service.NoticeService;
import com.campusone.notice.service.NotificationDistributionService;
import com.campusone.security.UserContext;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NoticeService {

    private final UserNotificationMapper userNotificationMapper;
    private final NotificationDistributionService notificationDistributionService;
    private final UserMapper userMapper;

    public Notification create(Notification notification) {
        if ("USER".equals(notification.getTargetType())) {
            User target = notification.getTargetId() == null
                    ? null
                    : userMapper.selectById(notification.getTargetId());
            if (target == null || !Integer.valueOf(1).equals(target.getStatus())) {
                throw new BusinessException("指定用户不存在或已停用");
            }
        }
        this.save(notification);
        notificationDistributionService.distributeAsync(
                notification.getId(), notification.getTargetType(), notification.getTargetId());
        return notification;
    }

    @Override
    public IPage<Notification> getMyNotices(Long userId, int page, int size, String type) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return baseMapper.selectUserNotificationPage(new Page<>(safePage, safeSize), userId, type);
    }

    @Override
    public Notification getVisibleNotice(Long id, Long userId) {
        UserNotification relation = userNotificationMapper.selectOne(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getNotificationId, id)
                        .eq(UserNotification::getUserId, userId));
        if (relation == null) {
            throw new BusinessException(403, "无权查看该通知");
        }
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        notification.setIsRead(Boolean.TRUE.equals(relation.getIsRead()) ? 1 : 0);
        return notification;
    }

    @Override
    public int getUnreadCount(Long userId) {
        return (int) userNotificationMapper.countUnread(userId);
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        userNotificationMapper.markAsRead(id, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        userNotificationMapper.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void deleteNotice(Long id) {
        Notification notification = getById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        String role = UserContext.getCurrentUserRole();
        Long operatorId = UserContext.getCurrentUserId();
        boolean admin = "ADMIN".equals(role) || "SUPER_ADMIN".equals(role);
        if (!admin && !operatorId.equals(notification.getSenderId())) {
            throw new BusinessException(403, "只能删除自己发布的通知");
        }
        userNotificationMapper.delete(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getNotificationId, id));
        removeById(id);
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
