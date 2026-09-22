package com.campusone.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusone.common.exception.BusinessException;
import com.campusone.reservation.entity.CampusResource;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.CampusResourceMapper;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.reservation.service.impl.ReservationServiceImpl;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ReservationServiceTest {

    @Mock
    private CampusResourceMapper resourceMapper;

    @Mock
    private ResourceReservationMapper reservationMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private CampusResource testResource;
    private ResourceReservation testReservation;

    @BeforeEach
    void setUpBaseMapper() {
        ReflectionTestUtils.setField(reservationService, "baseMapper", reservationMapper);
    }

    @BeforeEach
    void setUp() {
        testResource = new CampusResource();
        testResource.setId(1L);
        testResource.setResourceCode("RES001");
        testResource.setResourceName("软件实验室 305");
        testResource.setResourceType("lab");
        testResource.setCapacity(45);
        testResource.setStatus("AVAILABLE");

        testReservation = new ResourceReservation();
        testReservation.setResourceId(1L);
        testReservation.setReservationDate(LocalDate.now().plusDays(1));
        testReservation.setStartTime("14:00");
        testReservation.setEndTime("16:00");
        testReservation.setPurpose("自主学习");
        testReservation.setParticipantCount(5);
    }

    @Test
    @DisplayName("获取可用场地列表")
    void testListResources() {
        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(testResource));

        List<CampusResource> result = reservationService.listResources("lab", null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("软件实验室 305", result.get(0).getResourceName());
    }

    @Test
    @DisplayName("获取场地列表 - 空结果")
    void testListResources_Empty() {
        when(resourceMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of());

        List<CampusResource> result = reservationService.listResources("gym", null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("场地并发预约 - 成功场景")
    void testCreateReservation_Success() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(resourceMapper.selectById(1L)).thenReturn(testResource);
        when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(reservationMapper.insert(any(ResourceReservation.class))).thenReturn(1);

        ResourceReservation result = reservationService.createReservation(testReservation, 1L);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        assertNotNull(result.getReservationNo());
        assertTrue(result.getReservationNo().startsWith("RS"));
        assertEquals(1L, result.getUserId());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("场地并发预约 - 时段冲突")
    void testCreateReservation_Conflict() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(resourceMapper.selectById(1L)).thenReturn(testResource);
        when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> reservationService.createReservation(testReservation, 1L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("场地并发预约 - 获取锁失败")
    void testCreateReservation_LockFailed() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> reservationService.createReservation(testReservation, 1L));
        verify(lock, never()).unlock();
    }

    @Test
    @DisplayName("取消预约 - 正常取消（OWNER）")
    void testCancelReservation_Success() {
        testReservation.setId(1L);
        testReservation.setUserId(1L);
        testReservation.setStatus("CONFIRMED");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(reservationMapper.updateById((ResourceReservation) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertDoesNotThrow(() -> reservationService.cancelReservation(1L, 1L));
            assertEquals("CANCELLED", testReservation.getStatus());
        }
    }

    @Test
    @DisplayName("取消预约 - ADMIN可以取消他人预约")
    void testCancelReservation_AdminCanCancel() {
        testReservation.setId(1L);
        testReservation.setUserId(2L);
        testReservation.setStatus("PENDING");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(reservationMapper.updateById((ResourceReservation) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            assertDoesNotThrow(() -> reservationService.cancelReservation(1L, 1L));
            assertEquals("CANCELLED", testReservation.getStatus());
        }
    }

    @Test
    @DisplayName("取消预约 - 无权取消他人预约")
    void testCancelReservation_Unauthorized() {
        testReservation.setId(1L);
        testReservation.setUserId(2L);
        testReservation.setStatus("CONFIRMED");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertThrows(BusinessException.class,
                    () -> reservationService.cancelReservation(1L, 999L));
        }
    }

    @Test
    @DisplayName("取消预约 - 预约不存在")
    void testCancelReservation_NotFound() {
        when(reservationMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> reservationService.cancelReservation(999L, 1L));
    }

    @Test
    @DisplayName("取消预约 - 状态不允许（IN_USE）")
    void testCancelReservation_InvalidStatus() {
        testReservation.setId(1L);
        testReservation.setUserId(1L);
        testReservation.setStatus("IN_USE");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertThrows(BusinessException.class,
                    () -> reservationService.cancelReservation(1L, 1L));
        }
    }

    @Test
    @DisplayName("取消预约 - 状态不允许（CANCELLED）")
    void testCancelReservation_AlreadyCancelled() {
        testReservation.setId(1L);
        testReservation.setUserId(1L);
        testReservation.setStatus("CANCELLED");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertThrows(BusinessException.class,
                    () -> reservationService.cancelReservation(1L, 1L));
        }
    }

    @Test
    @DisplayName("PENDING状态也允许取消")
    void testCancelReservation_PendingStatus() {
        testReservation.setId(1L);
        testReservation.setUserId(1L);
        testReservation.setStatus("PENDING");
        when(reservationMapper.selectById(1L)).thenReturn(testReservation);
        when(reservationMapper.updateById((ResourceReservation) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertDoesNotThrow(() -> reservationService.cancelReservation(1L, 1L));
            assertEquals("CANCELLED", testReservation.getStatus());
        }
    }

    @Test
    @DisplayName("预约时间 - 结束时间晚于开始时间校验通过")
    void testCreateReservation_ValidTimeRange() throws Exception {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(resourceMapper.selectById(1L)).thenReturn(testResource);
        when(reservationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(reservationMapper.insert(any(ResourceReservation.class))).thenReturn(1);

        ResourceReservation result = reservationService.createReservation(testReservation, 1L);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("预约时间 - 结束时间早于开始时间抛异常")
    void testCreateReservation_EndBeforeStart() throws Exception {
        testReservation.setStartTime("16:00");
        testReservation.setEndTime("14:00");
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(testReservation, 1L));
        assertEquals("预约结束时间必须晚于开始时间", ex.getMessage());
        verify(lock).unlock();
    }

    @Test
    @DisplayName("预约时间 - 结束时间等于开始时间抛异常")
    void testCreateReservation_EqualTimeRange() throws Exception {
        testReservation.setEndTime("14:00");
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> reservationService.createReservation(testReservation, 1L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("预约时间 - 时间为空抛异常")
    void testCreateReservation_EmptyTime() throws Exception {
        testReservation.setStartTime(null);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> reservationService.createReservation(testReservation, 1L));
        verify(lock).unlock();
    }

    @Test
    @DisplayName("预约时间 - 时间格式非法抛异常")
    void testCreateReservation_InvalidTimeFormat() throws Exception {
        testReservation.setStartTime("14:00");
        testReservation.setEndTime("abc");
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any())).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.createReservation(testReservation, 1L));
        assertEquals("预约时间格式不正确", ex.getMessage());
        verify(lock).unlock();
    }
}
