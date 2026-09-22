package com.campusone.notice.service;

import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDistributionService {
    private final UserMapper userMapper;
    private final UserNotificationMapper userNotificationMapper;

    @Async("notificationExecutor")
    public void distributeAsync(Long notificationId, String targetType, Long targetId) {
        if ("ALL".equals(targetType)) {
            List<User> users = userMapper.selectList(null);
            for (User user : users) {
                createUserNotification(notificationId, user.getId());
            }
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
