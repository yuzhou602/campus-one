package com.campusone.task.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusone.application.entity.ServiceApplication;
import com.campusone.application.entity.ApprovalRecord;
import com.campusone.application.mapper.ServiceApplicationMapper;
import com.campusone.application.mapper.ApprovalRecordMapper;
import com.campusone.common.response.ApiResponse;
import com.campusone.common.response.PageResult;
import com.campusone.repair.entity.RepairOrder;
import com.campusone.repair.mapper.RepairOrderMapper;
import com.campusone.reservation.entity.ResourceReservation;
import com.campusone.reservation.mapper.ResourceReservationMapper;
import com.campusone.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Tag(name = "任务中心")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final ServiceApplicationMapper applicationMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final ResourceReservationMapper reservationMapper;

    @Operation(summary = "我的待办")
    @GetMapping("/my")
    public ApiResponse<Map<String, Object>> myTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        Map<String, Object> tasks = new HashMap<>();

        List<Long> assignedApplicationIds = approvalRecordMapper.selectList(
                new LambdaQueryWrapper<ApprovalRecord>()
                        .eq(ApprovalRecord::getAssigneeId, userId)
                        .eq(ApprovalRecord::getAction, "PENDING"))
                .stream()
                .map(ApprovalRecord::getApplicationId)
                .distinct()
                .toList();
        IPage<ServiceApplication> approvals;
        if (assignedApplicationIds.isEmpty()) {
            approvals = new Page<>(page, pageSize);
        } else {
            LambdaQueryWrapper<ServiceApplication> approvalWrapper = new LambdaQueryWrapper<>();
            approvalWrapper.in(ServiceApplication::getId, assignedApplicationIds)
                    .eq(ServiceApplication::getStatus, "PENDING")
                    .orderByDesc(ServiceApplication::getCreatedAt);
            approvals = applicationMapper.selectPage(new Page<>(page, pageSize), approvalWrapper);
        }
        tasks.put("pendingApprovals", PageResult.of(approvals));

        LambdaQueryWrapper<ResourceReservation> reservationWrapper = new LambdaQueryWrapper<>();
        reservationWrapper.eq(ResourceReservation::getUserId, userId);
        reservationWrapper.eq(ResourceReservation::getStatus, "CONFIRMED");
        reservationWrapper.ge(ResourceReservation::getReservationDate, java.time.LocalDate.now());
        reservationWrapper.orderByAsc(ResourceReservation::getReservationDate);
        IPage<ResourceReservation> reservations = reservationMapper.selectPage(
            new Page<>(page, pageSize), reservationWrapper);
        tasks.put("myReservations", PageResult.of(reservations));

        LambdaQueryWrapper<RepairOrder> repairWrapper = new LambdaQueryWrapper<>();
        repairWrapper.eq(RepairOrder::getUserId, userId);
        repairWrapper.notIn(RepairOrder::getStatus, "RESOLVED", "CLOSED");
        repairWrapper.orderByDesc(RepairOrder::getCreatedAt);
        IPage<RepairOrder> repairs = repairOrderMapper.selectPage(
            new Page<>(page, pageSize), repairWrapper);
        tasks.put("myRepairs", PageResult.of(repairs));

        return ApiResponse.success(tasks);
    }
}
