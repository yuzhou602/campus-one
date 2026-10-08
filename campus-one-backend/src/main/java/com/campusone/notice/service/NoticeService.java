package com.campusone.notice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.campusone.notice.entity.Notification;

public interface NoticeService extends IService<Notification> {
    IPage<Notification> getMyNotices(Long userId, int page, int size, String type);
    Notification getVisibleNotice(Long id, Long userId);
    int getUnreadCount(Long userId);
    void markAsRead(Long id, Long userId);
    void markAllAsRead(Long userId);
    void deleteNotice(Long id);
}
