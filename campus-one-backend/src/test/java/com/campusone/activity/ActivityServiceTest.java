package com.campusone.activity;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.activity.entity.ActivityRegistration;
import com.campusone.activity.entity.CampusActivity;
import com.campusone.activity.mapper.ActivityRegistrationMapper;
import com.campusone.activity.mapper.CampusActivityMapper;
import com.campusone.activity.service.impl.ActivityServiceImpl;
import com.campusone.common.exception.BusinessException;
import com.campusone.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ActivityServiceTest {

    @Mock
    private CampusActivityMapper activityMapper;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private ActivityServiceImpl activityService;

    private CampusActivity testActivity;

    @BeforeEach
    void setUpBaseMapper() {
        ReflectionTestUtils.setField(activityService, "baseMapper", activityMapper);
    }

    @BeforeEach
    void setUp() {
        testActivity = new CampusActivity();
        testActivity.setId(1L);
        testActivity.setTitle("AI讲座");
        testActivity.setCapacity(200);
        testActivity.setRegisteredCount(199);
        testActivity.setStatus("ACTIVE");
        testActivity.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        testActivity.setStartTime(LocalDateTime.now().minusMinutes(5));
        testActivity.setEndTime(LocalDateTime.now().plusHours(2));
        testActivity.setCreatorId(10L);
    }

    @Test
    @DisplayName("活动报名 - 成功")
    void testRegisterActivity_Success() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(activityMapper.selectById(1L)).thenReturn(testActivity);
        when(registrationMapper.insert(any(ActivityRegistration.class))).thenReturn(1);
        when(activityMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> activityService.registerActivity(1L, 10L));
        verify(registrationMapper).insert(any(ActivityRegistration.class));
        verify(activityMapper).update(any(), any());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("活动报名 - 重复报名")
    void testRegisterActivity_Duplicate() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> activityService.registerActivity(1L, 10L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("活动报名 - 名额已满")
    void testRegisterActivity_Full() throws Exception {
        testActivity.setRegisteredCount(200);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(activityMapper.selectById(1L)).thenReturn(testActivity);

        assertThrows(BusinessException.class,
                () -> activityService.registerActivity(1L, 10L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("活动报名 - 活动不存在")
    void testRegisterActivity_NotFound() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(activityMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> activityService.registerActivity(999L, 10L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("活动报名 - 报名已截止")
    void testRegisterActivity_DeadlinePassed() throws Exception {
        testActivity.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(registrationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(activityMapper.selectById(1L)).thenReturn(testActivity);

        assertThrows(BusinessException.class,
                () -> activityService.registerActivity(1L, 10L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("活动报名 - 获取锁失败")
    void testRegisterActivity_LockFailed() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> activityService.registerActivity(1L, 10L));
        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("取消报名 - 正常取消")
    void testCancelRegistration_Success() {
        ActivityRegistration reg = new ActivityRegistration();
        reg.setId(1L);
        reg.setActivityId(1L);
        reg.setUserId(10L);
        when(registrationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reg);
        when(registrationMapper.deleteById(1L)).thenReturn(1);
        when(activityMapper.update(any(), any())).thenReturn(1);

        assertDoesNotThrow(() -> activityService.cancelRegistration(1L, 10L));
        verify(registrationMapper).deleteById(1L);
        verify(activityMapper).update(any(), any());
    }

    @Test
    @DisplayName("取消报名 - 未报名")
    void testCancelRegistration_NotRegistered() {
        when(registrationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> activityService.cancelRegistration(1L, 10L));
    }

    @Test
    @DisplayName("签到 - 正常签到")
    void testCheckin_Success() {
        ActivityRegistration reg = new ActivityRegistration();
        reg.setId(1L);
        reg.setActivityId(1L);
        reg.setUserId(10L);
        reg.setCheckedIn(false);
        when(registrationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reg);
        when(activityMapper.selectById(1L)).thenReturn(testActivity);
        when(registrationMapper.updateById((ActivityRegistration) any())).thenReturn(1);

        assertDoesNotThrow(() -> activityService.checkin(1L, 10L));
        assertTrue(reg.getCheckedIn());
        assertNotNull(reg.getCheckedInAt());
        verify(registrationMapper).updateById(reg);
    }

    @Test
    @DisplayName("签到 - 未报名不能签到")
    void testCheckin_NotRegistered() {
        when(registrationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> activityService.checkin(1L, 10L));
    }

    @Test
    @DisplayName("签到 - 不允许重复签到")
    void testCheckin_AlreadyCheckedIn() {
        ActivityRegistration reg = new ActivityRegistration();
        reg.setCheckedIn(true);
        when(registrationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reg);

        BusinessException error = assertThrows(BusinessException.class,
                () -> activityService.checkin(1L, 10L));

        assertEquals("请勿重复签到", error.getMessage());
        verify(registrationMapper, never()).updateById(any(ActivityRegistration.class));
    }

    @Test
    @DisplayName("签到 - 开始前超过30分钟不能签到")
    void testCheckin_TooEarly() {
        ActivityRegistration reg = new ActivityRegistration();
        reg.setCheckedIn(false);
        testActivity.setStartTime(LocalDateTime.now().plusHours(2));
        when(registrationMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(reg);
        when(activityMapper.selectById(1L)).thenReturn(testActivity);

        BusinessException error = assertThrows(BusinessException.class,
                () -> activityService.checkin(1L, 10L));

        assertEquals("签到尚未开始", error.getMessage());
    }

    @Test
    @DisplayName("更新活动 - ADMIN可以更新")
    void testUpdateActivity_Admin() {
        CampusActivity update = new CampusActivity();
        update.setTitle("更新后的标题");
        when(activityMapper.selectById(1L)).thenReturn(testActivity);
        when(activityMapper.updateById((CampusActivity) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            CampusActivity result = activityService.updateActivity(1L, update);
            assertEquals("更新后的标题", result.getTitle());
        }
    }

    @Test
    @DisplayName("更新活动 - 创建者可以更新")
    void testUpdateActivity_Creator() {
        CampusActivity update = new CampusActivity();
        update.setTitle("更新后的标题");
        when(activityMapper.selectById(1L)).thenReturn(testActivity);
        when(activityMapper.updateById((CampusActivity) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");
            userContext.when(UserContext::getCurrentUserId).thenReturn(10L);

            CampusActivity result = activityService.updateActivity(1L, update);
            assertEquals("更新后的标题", result.getTitle());
        }
    }

    @Test
    @DisplayName("更新活动 - 无权更新")
    void testUpdateActivity_Unauthorized() {
        CampusActivity update = new CampusActivity();
        update.setTitle("更新后的标题");
        when(activityMapper.selectById(1L)).thenReturn(testActivity);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");
            userContext.when(UserContext::getCurrentUserId).thenReturn(999L);

            assertThrows(BusinessException.class,
                    () -> activityService.updateActivity(1L, update));
        }
    }

    @Test
    @DisplayName("更新活动 - 活动不存在")
    void testUpdateActivity_NotFound() {
        when(activityMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> activityService.updateActivity(999L, new CampusActivity()));
    }

    @Test
    @DisplayName("删除活动 - ADMIN可以删除")
    void testDeleteActivity_Admin() {
        when(activityMapper.selectById(1L)).thenReturn(testActivity);
        when(activityMapper.deleteById(1L)).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            assertDoesNotThrow(() -> activityService.deleteActivity(1L));
        }
    }

    @Test
    @DisplayName("删除活动 - 无权删除")
    void testDeleteActivity_Unauthorized() {
        when(activityMapper.selectById(1L)).thenReturn(testActivity);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");
            userContext.when(UserContext::getCurrentUserId).thenReturn(999L);

            assertThrows(BusinessException.class,
                    () -> activityService.deleteActivity(1L));
        }
    }

    @Test
    @DisplayName("删除活动 - 活动不存在")
    void testDeleteActivity_NotFound() {
        when(activityMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> activityService.deleteActivity(999L));
    }

    @Test
    @DisplayName("获取活动列表 - 正常")
    void testListActivities() {
        Page<CampusActivity> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(testActivity));
        when(activityMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<CampusActivity> result = activityService.listActivities(1, 10, "all");

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("获取活动列表 - 按分类筛选")
    void testListActivities_ByCategory() {
        Page<CampusActivity> page = new Page<>(1, 10);
        when(activityMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<CampusActivity> result = activityService.listActivities(1, 10, "academic");

        assertNotNull(result);
        verify(activityMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }
}
