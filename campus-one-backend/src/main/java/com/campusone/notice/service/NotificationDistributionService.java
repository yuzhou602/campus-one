package com.campusone.notice.service;

import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.UserNotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationDistributionService {
    private final UserNotificationMapper userNotificationMapper;

    @Async("notificationExecutor")
    public void distributeAsync(Long notificationId, String targetType, Long targetId) {
        if ("ALL".equals(targetType)) {
            userNotificationMapper.distributeToAllActiveUsers(notificationId);
        } else if ("USER".equals(targetType) && targetId != null) {
            createUserNotification(notificationId, targetId);
        }
    }

    private void createUserNotification(Long notificationId, Long userId) {
        UserNotification userNotification = new UserNotification();
        userNotification.setNotificationId(notificationId);
        userNotification.setUserId(userId);
        userNotification.setIsRead(false);
        userNotificationMapper.insert(userNotification);
    }
}
