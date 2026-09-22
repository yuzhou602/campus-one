package com.campusone.repair;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.common.exception.BusinessException;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.repair.service.impl.RepairServiceImpl;
import com.campusone.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class RepairServiceTest {

    @Mock
    private RepairOrderMapper repairOrderMapper;

    @InjectMocks
    private RepairServiceImpl repairService;

    private RepairOrder testOrder;

    @BeforeEach
    void setUpBaseMapper() {
        ReflectionTestUtils.setField(repairService, "baseMapper", repairOrderMapper);
    }

    @BeforeEach
    void setUp() {
        testOrder = new RepairOrder();
        testOrder.setId(1L);
        testOrder.setRepairNo("RP20260906001");
        testOrder.setUserId(100L);
        testOrder.setLocation("学生宿舍12号楼301");
        testOrder.setCategory("dorm");
        testOrder.setDescription("宿舍空调不制冷");
        testOrder.setContact("13888888888");
        testOrder.setStatus("SUBMITTED");
    }

    @Test
    @DisplayName("创建报修工单 - 保存并返回")
    void testCreateRepair() {
        when(repairOrderMapper.insert(any(RepairOrder.class))).thenReturn(1);

        RepairOrder result = repairService.createRepair(testOrder);

        assertNotNull(result);
        verify(repairOrderMapper).insert(testOrder);
    }

    @Test
    @DisplayName("获取我的报修列表")
    void testGetMyRepairs() {
        Page<RepairOrder> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(testOrder));
        when(repairOrderMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<RepairOrder> result = repairService.getMyRepairs(100L, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getRecords().size());
        verify(repairOrderMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("获取分配给我的报修列表")
    void testGetAssignedRepairs() {
        Page<RepairOrder> page = new Page<>(1, 10);
        page.setRecords(java.util.List.of(testOrder));
        when(repairOrderMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(page);

        IPage<RepairOrder> result = repairService.getAssignedRepairs(200L, 1, 10);

        assertEquals(1, result.getRecords().size());
        verify(repairOrderMapper).selectPage(any(), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("分配工单 - 同时记录维修人员和状态")
    void testAssignRepair() {
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById(any(RepairOrder.class))).thenReturn(1);

        repairService.assignRepair(1L, 200L);

        assertEquals(200L, testOrder.getAssignedUserId());
        assertEquals("ASSIGNED", testOrder.getStatus());
        verify(repairOrderMapper).updateById(testOrder);
    }

    @Test
    @DisplayName("根据ID获取报修单")
    void testGetRepairById() {
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);

        RepairOrder result = repairService.getRepairById(1L);

        assertNotNull(result);
        assertEquals("RP20260906001", result.getRepairNo());
    }

    @Test
    @DisplayName("接单 - 正常接单")
    void testAcceptRepair_Success() {
        testOrder.setStatus("ASSIGNED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("SERVICE");

            assertDoesNotThrow(() -> repairService.acceptRepair(1L, 2L));
            assertEquals("ACCEPTED", testOrder.getStatus());
            assertEquals(2L, testOrder.getAssignedUserId());
            assertNotNull(testOrder.getAcceptedAt());
            verify(repairOrderMapper).updateById(testOrder);
        }
    }

    @Test
    @DisplayName("接单 - 工单不存在")
    void testAcceptRepair_OrderNotFound() {
        when(repairOrderMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> repairService.acceptRepair(999L, 2L));
    }

    @Test
    @DisplayName("接单 - 状态不是ASSIGNED")
    void testAcceptRepair_InvalidStatus() {
        testOrder.setStatus("SUBMITTED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);

        assertThrows(BusinessException.class,
                () -> repairService.acceptRepair(1L, 2L));
    }

    @Test
    @DisplayName("接单 - 非ADMIN/SERVICE且非指定人")
    void testAcceptRepair_Unauthorized() {
        testOrder.setStatus("ASSIGNED");
        testOrder.setAssignedUserId(5L);
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertThrows(BusinessException.class,
                    () -> repairService.acceptRepair(1L, 999L));
        }
    }

    @Test
    @DisplayName("状态流转 - SUBMITTED -> ASSIGNED")
    void testStatusTransition_SubmittedToAssigned() {
        testOrder.setStatus("SUBMITTED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            RepairOrder result = repairService.updateStatus(1L, "ASSIGNED", 2L);
            assertEquals("ASSIGNED", result.getStatus());
        }
    }

    @Test
    @DisplayName("状态流转 - ASSIGNED -> ACCEPTED")
    void testStatusTransition_AssignedToAccepted() {
        testOrder.setStatus("ASSIGNED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            RepairOrder result = repairService.updateStatus(1L, "ACCEPTED", 2L);
            assertEquals("ACCEPTED", result.getStatus());
            assertNotNull(result.getAcceptedAt());
        }
    }

    @Test
    @DisplayName("状态流转 - ACCEPTED -> PROCESSING")
    void testStatusTransition_AcceptedToProcessing() {
        testOrder.setStatus("ACCEPTED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("SERVICE");

            RepairOrder result = repairService.updateStatus(1L, "PROCESSING", 2L);
            assertEquals("PROCESSING", result.getStatus());
        }
    }

    @Test
    @DisplayName("状态流转 - PROCESSING -> RESOLVED")
    void testStatusTransition_ProcessingToResolved() {
        testOrder.setStatus("PROCESSING");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("SERVICE");

            RepairOrder result = repairService.updateStatus(1L, "RESOLVED", 2L);
            assertEquals("RESOLVED", result.getStatus());
            assertNotNull(result.getResolvedAt());
        }
    }

    @Test
    @DisplayName("状态流转 - RESOLVED -> CONFIRMED")
    void testStatusTransition_ResolvedToConfirmed() {
        testOrder.setStatus("RESOLVED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            RepairOrder result = repairService.updateStatus(1L, "CONFIRMED", 2L);
            assertEquals("CONFIRMED", result.getStatus());
        }
    }

    @Test
    @DisplayName("状态流转 - CONFIRMED -> CLOSED")
    void testStatusTransition_ConfirmedToClosed() {
        testOrder.setStatus("CONFIRMED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);
        when(repairOrderMapper.updateById((RepairOrder) any())).thenReturn(1);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("ADMIN");

            RepairOrder result = repairService.updateStatus(1L, "CLOSED", 2L);
            assertEquals("CLOSED", result.getStatus());
        }
    }

    @Test
    @DisplayName("状态流转 - 非法状态转换")
    void testStatusTransition_Invalid() {
        testOrder.setStatus("CLOSED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);

        assertThrows(BusinessException.class,
                () -> repairService.updateStatus(1L, "SUBMITTED", 2L));
    }

    @Test
    @DisplayName("状态流转 - 跳过状态")
    void testStatusTransition_SkipState() {
        testOrder.setStatus("SUBMITTED");
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);

        assertThrows(BusinessException.class,
                () -> repairService.updateStatus(1L, "ACCEPTED", 2L));
    }

    @Test
    @DisplayName("updateStatus - 工单不存在")
    void testUpdateStatus_OrderNotFound() {
        when(repairOrderMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> repairService.updateStatus(999L, "ASSIGNED", 2L));
    }

    @Test
    @DisplayName("updateStatus - 非ADMIN/SERVICE且非指定人")
    void testUpdateStatus_Unauthorized() {
        testOrder.setStatus("SUBMITTED");
        testOrder.setAssignedUserId(5L);
        when(repairOrderMapper.selectById(1L)).thenReturn(testOrder);

        try (MockedStatic<UserContext> userContext = mockStatic(UserContext.class)) {
            userContext.when(UserContext::getCurrentUserRole).thenReturn("STUDENT");

            assertThrows(BusinessException.class,
                    () -> repairService.updateStatus(1L, "ASSIGNED", 999L));
        }
    }
}
