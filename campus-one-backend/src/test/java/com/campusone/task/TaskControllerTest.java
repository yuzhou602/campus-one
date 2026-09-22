package com.campusone.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.application.entity.ApprovalRecord;
import com.campusone.application.entity.ServiceApplication;
import com.campusone.application.mapper.ApprovalRecordMapper;
import com.campusone.application.mapper.ServiceApplicationMapper;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.security.UserContext;
import com.campusone.task.controller.TaskController;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskControllerTest {
    @Test
    @SuppressWarnings("unchecked")
    void onlyReturnsApprovalsAssignedToCurrentUser() {
        ServiceApplicationMapper applicationMapper = mock(ServiceApplicationMapper.class);
        ApprovalRecordMapper approvalRecordMapper = mock(ApprovalRecordMapper.class);
        RepairOrderMapper repairMapper = mock(RepairOrderMapper.class);
        ResourceReservationMapper reservationMapper = mock(ResourceReservationMapper.class);
        TaskController controller = new TaskController(
                applicationMapper, approvalRecordMapper, repairMapper, reservationMapper);

        ApprovalRecord assigned = new ApprovalRecord();
        assigned.setApplicationId(99L);
        assigned.setAssigneeId(7L);
        assigned.setAction("PENDING");
        when(approvalRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(assigned));

        ServiceApplication application = new ServiceApplication();
        application.setId(99L);
        Page<ServiceApplication> applicationPage = new Page<>(1, 10);
        applicationPage.setRecords(List.of(application));
        when(applicationMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(applicationPage);
        when(reservationMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(new Page<ResourceReservation>(1, 10));
        when(repairMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(new Page<RepairOrder>(1, 10));

        try (MockedStatic<UserContext> context = mockStatic(UserContext.class)) {
            context.when(UserContext::getCurrentUserId).thenReturn(7L);

            var response = controller.myTasks(1, 10);

            assertNotNull(response.getData().get("pendingApprovals"));
            verify(approvalRecordMapper).selectList(any(LambdaQueryWrapper.class));
            verify(applicationMapper).selectPage(any(), any(LambdaQueryWrapper.class));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void skipsApplicationQueryWhenUserHasNoAssignedApprovals() {
        ServiceApplicationMapper applicationMapper = mock(ServiceApplicationMapper.class);
        ApprovalRecordMapper approvalRecordMapper = mock(ApprovalRecordMapper.class);
        RepairOrderMapper repairMapper = mock(RepairOrderMapper.class);
        ResourceReservationMapper reservationMapper = mock(ResourceReservationMapper.class);
        TaskController controller = new TaskController(
                applicationMapper, approvalRecordMapper, repairMapper, reservationMapper);
        when(approvalRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(reservationMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(new Page<ResourceReservation>(1, 10));
        when(repairMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(new Page<RepairOrder>(1, 10));

        try (MockedStatic<UserContext> context = mockStatic(UserContext.class)) {
            context.when(UserContext::getCurrentUserId).thenReturn(7L);
            controller.myTasks(1, 10);
        }

        verify(applicationMapper, never()).selectPage(any(), any());
    }
}
