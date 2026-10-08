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
    private NotificationDistributionService notificationDistributionService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    private NotificationDistributionService distributionService;

    private Notification testNotification;

    @BeforeEach
    void setUpBaseMapper() {
        ReflectionTestUtils.setField(noticeService, "baseMapper", notificationMapper);
        distributionService = new NotificationDistributionService(userNotificationMapper);
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
    @DisplayName("创建指定用户通知 - 拒绝不存在或停用账号")
    void testCreateRejectsUnavailableTargetUser() {
        testNotification.setTargetType("USER");
        testNotification.setTargetId(99L);
        when(userMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> noticeService.create(testNotification));

        verify(notificationMapper, never()).insert(any(Notification.class));
        verify(notificationDistributionService, never()).distributeAsync(any(), any(), any());
    }

    @Test
    @DisplayName("获取我的通知 - ALL类型")
    void testGetMyNotices_All() {
        Page<Notification> page = new Page<>(1, 10);
        page.setRecords(List.of(testNotification));
        when(notificationMapper.selectUserNotificationPage(any(Page.class), eq(1L), eq("all"))).thenReturn(page);

        IPage<Notification> result = noticeService.getMyNotices(1L, 1, 10, "all");

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("获取我的通知 - 按类型筛选")
    void testGetMyNotices_ByType() {
        Page<Notification> page = new Page<>(1, 10);
        when(notificationMapper.selectUserNotificationPage(any(Page.class), eq(1L), eq("SYSTEM"))).thenReturn(page);

        IPage<Notification> result = noticeService.getMyNotices(1L, 1, 10, "SYSTEM");

        assertNotNull(result);
        verify(notificationMapper).selectUserNotificationPage(any(Page.class), eq(1L), eq("SYSTEM"));
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
    @DisplayName("查看通知 - 必须存在用户投递关系")
    void testGetVisibleNotice() {
        UserNotification relation = new UserNotification();
        relation.setIsRead(true);
        when(userNotificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(relation);
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);

        Notification result = noticeService.getVisibleNotice(1L, 7L);

        assertSame(testNotification, result);
        assertEquals(1, result.getIsRead());
    }

    @Test
    @DisplayName("查看通知 - 无投递关系时拒绝")
    void testGetVisibleNoticeForbidden() {
        when(userNotificationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> noticeService.getVisibleNotice(1L, 7L));

        assertEquals(403, error.getCode());
        verify(notificationMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("删除通知 - 先删除用户关系再删除通知")
    void testDeleteNotice() {
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);
        when(userNotificationMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
        when(notificationMapper.deleteById(1L)).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("SUPER_ADMIN");
            userContext.when(UserContext::getCurrentUserId).thenReturn(9L);
            noticeService.deleteNotice(1L);
        }

        var order = inOrder(userNotificationMapper, notificationMapper);
        order.verify(userNotificationMapper).delete(any(LambdaQueryWrapper.class));
        order.verify(notificationMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除通知 - 辅导员只能删除自己发布的通知")
    void testDeleteNoticeRejectsOtherPublisher() {
        testNotification.setSenderId(8L);
        when(notificationMapper.selectById(1L)).thenReturn(testNotification);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("COUNSELOR");
            userContext.when(UserContext::getCurrentUserId).thenReturn(9L);

            BusinessException error = assertThrows(BusinessException.class,
                    () -> noticeService.deleteNotice(1L));
            assertEquals(403, error.getCode());
        }

        verify(userNotificationMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(notificationMapper, never()).deleteById(any(Long.class));
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
        when(userNotificationMapper.distributeToAllActiveUsers(1L)).thenReturn(2);

        distributionService.distributeAsync(1L, "ALL", null);

        verify(userNotificationMapper).distributeToAllActiveUsers(1L);
        verify(userNotificationMapper, never()).insert(any(UserNotification.class));
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
        verify(userNotificationMapper, never()).distributeToAllActiveUsers(any());
    }
}
