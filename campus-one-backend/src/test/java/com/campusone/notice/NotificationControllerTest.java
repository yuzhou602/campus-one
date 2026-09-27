package com.campusone.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusone.common.exception.BusinessException;
import com.campusone.notice.controller.NotificationController;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.NotificationMapper;
import com.campusone.notice.mapper.UserNotificationMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationControllerTest {

    @Test
    void deleteRemovesUserRelationsBeforeNotification() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        UserNotificationMapper userNotificationMapper = mock(UserNotificationMapper.class);
        Notification notification = new Notification();
        notification.setId(9L);
        when(notificationMapper.selectById(9L)).thenReturn(notification);
        NotificationController controller = new NotificationController(notificationMapper, userNotificationMapper);

        controller.delete(9L);

        InOrder order = inOrder(userNotificationMapper, notificationMapper);
        order.verify(userNotificationMapper).delete(any(LambdaQueryWrapper.class));
        order.verify(notificationMapper).deleteById(9L);
    }

    @Test
    void deleteRejectsMissingNotification() {
        NotificationMapper notificationMapper = mock(NotificationMapper.class);
        UserNotificationMapper userNotificationMapper = mock(UserNotificationMapper.class);
        when(notificationMapper.selectById(9L)).thenReturn(null);
        NotificationController controller = new NotificationController(notificationMapper, userNotificationMapper);

        assertThrows(BusinessException.class, () -> controller.delete(9L));

        verify(userNotificationMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(notificationMapper, never()).deleteById(any(Serializable.class));
    }
}
