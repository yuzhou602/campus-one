package com.campusone.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.common.exception.BusinessException;
import com.campusone.notice.entity.Notification;
import com.campusone.notice.entity.UserNotification;
import com.campusone.notice.mapper.NotificationMapper;
import com.campusone.notice.mapper.UserNotificationMapper;
import com.campusone.notice.service.NotificationDistributionService;
import com.campusone.notice.service.impl.NoticeServiceImpl;
import com.campusone.security.UserContext;
import com.campusone.system.user.entity.User;
import com.campusone.system.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class NoticeServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private UserNotificationMapper userNotificationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private NotificationDistributionService notificationDistributionService;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private NotificationDistributionService distributionService;

    private Notification testNotification;

    @BeforeEach
    void setUpBaseMapper() {
        ReflectionTestUtils.setField(noticeService, "baseMapper", notificationMapper);
        distributionService = new NotificationDistributionService(userMapper, userNotificationMapper);
    }

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setTitle("系统通知");
        testNotification.setContent("欢迎使用智慧校园平台");
        testNotification.setType("SYSTEM");
        testNotification.setTargetType("ALL");
        testNotification.setIsRead(0);
    }

    @Test
    @DisplayName("创建通知 - 成功")
    void testCreate() {
        when(notificationMapper.insert(any(Notification.class))).thenReturn(1);

        Notification result = noticeService.create(testNotification);

        assertNotNull(result);
        verify(notificationMapper).insert(testNotification);
        verify(notificationDistributionService).distributeAsync(1L, "ALL", null);
    }

    @Test
    @DisplayName("获取我的通知 - ALL类型")
    void testGetMyNotices_All() {
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of(testNotification));
        when(notificationMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<Notification> result = noticeService.getMyNotices(1L, 1, 10, "all");

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("获取我的通知 - 按类型筛选")
    void testGetMyNotices_ByType() {
        Page<Notification> page = new Page<>(1, 10);
        when(notificationMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<Notification> result = noticeService.getMyNotices(1L, 1, 10, "SYSTEM");

        assertNotNull(result);
        verify(notificationMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取未读数量")
    void testGetUnreadCount() {
        when(userNotificationMapper.countUnread(1L)).thenReturn(5L);

        int count = noticeService.getUnreadCount(1L);

        assertEquals(5, count);
    }

    @Test
    @DisplayName("标记已读")
    void testMarkAsRead() {
        when(userNotificationMapper.markAsRead(1L, 1L)).thenReturn(1);

        assertDoesNotThrow(() -> noticeService.markAsRead(1L, 1L));
        verify(userNotificationMapper).markAsRead(1L, 1L);
    }

    @Test
    @DisplayName("标记已读 - 通知不存在不报错")
    void testMarkAsRead_NotFound() {
        when(userNotificationMapper.markAsRead(999L, 1L)).thenReturn(0);

        assertDoesNotThrow(() -> noticeService.markAsRead(999L, 1L));
    }

    @Test
    @DisplayName("更新通知 - ADMIN可以更新")
    void testUpdateNotice_Admin() {
        Notification update = new Notification();
        update.setTitle("更新后的标题");
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);
        when(notificationMapper.updateById((Notification) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            assertDoesNotThrow(() -> noticeService.updateNotice(1L, update));
        }
    }

    @Test
    @DisplayName("更新通知 - COUNSELOR可以更新")
    void testUpdateNotice_Counselor() {
        Notification update = new Notification();
        update.setTitle("更新后的标题");
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);
        when(notificationMapper.updateById((Notification) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("COUNSELOR");

            assertDoesNotThrow(() -> noticeService.updateNotice(1L, update));
        }
    }

    @Test
    @DisplayName("更新通知 - 无权修改")
    void testUpdateNotice_Unauthorized() {
        Notification update = new Notification();
        update.setTitle("更新后的标题");
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertThrows(BusinessException.class,
                    () -> noticeService.updateNotice(1L, update));
        }
    }

    @Test
    @DisplayName("更新通知 - 通知不存在")
    void testUpdateNotice_NotFound() {
        when(notificationMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> noticeService.updateNotice(999L, new Notification()));
    }

    @Test
    @DisplayName("异步分发 - ALL类型通知")
    void testDistributeAsync_All() {
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        when(userMapper.selectList(null)).thenReturn(List.of(user1, user2));
        when(userNotificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        distributionService.distributeAsync(1L, "ALL", null);

        verify(userNotificationMapper, times(2)).insert(any(UserNotification.class));
    }

    @Test
    @DisplayName("异步分发 - USER类型通知")
    void testDistributeAsync_User() {
        when(userNotificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        distributionService.distributeAsync(1L, "USER", 10L);

        verify(userNotificationMapper, times(1)).insert(any(UserNotification.class));
    }

    @Test
    @DisplayName("异步分发 - 未知类型不报错")
    void testDistributeAsync_UnknownType() {
        assertDoesNotThrow(() -> distributionService.distributeAsync(1L, "UNKNOWN", null));
        verify(userNotificationMapper, never()).insert((UserNotification) any());
    }
}
